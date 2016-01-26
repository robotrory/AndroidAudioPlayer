package com.smithyproductions.audioplayer.MediaRouter;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.images.WebImage;
import com.smithyproductions.audioplayer.AudioTrack;
import com.smithyproductions.audioplayer.Turntable;
import com.smithyproductions.audioplayer.interfaces.AudioEngineCallbacks;
import com.smithyproductions.audioplayer.interfaces.AudioEngineInterface;
import com.smithyproductions.audioplayer.playerEngines.AnimatorRunnable;
import com.smithyproductions.audioplayer.trackProviders.TrackProvider;

import java.io.IOException;

/**
 * Created by rory on 25/01/16.
 */
public class MediaRouteManager implements AudioEngineInterface, TrackProvider.TrackProviderListener {

    private static final String TAG = "MediaRouteManager";
    public static final String APPLICATION_ID = "CC81EAFB";
    private final MediaRouter mMediaRouter;
    private final MediaRouteSelector mMediaRouteSelector;
    private final MyMediaRouterCallback mMediaRouterCallback;
    private final Context mContext;
    private final Cast.Listener mCastClientListener;
    private final RemoteMediaPlayer mRemoteMediaPlayer;
    private final MultiplexerControlInterface mCallbacks;
    private final AnimatorRunnable animatorRunnable;
    private boolean enabled;
    private CastDevice mSelectedDevice;
    private GoogleApiClient mApiClient;
    private boolean mApplicationStarted;
    private boolean mWaitingForReconnect;
    private String mSessionId;

    private static UIComponent sUIComponent;
    private boolean mAutoPlay;
    private AudioEngineCallbacks parentCallbacks;
    private TrackProvider trackProvider;
    private boolean mWaitingToLoad;
    private int mRequestedSeekPosition;
    private MediaInfo currentMediaInfo;

    public class UIComponent {

        public void beginScan() {
            mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                    MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
        }

        public void endScan() {
            mMediaRouter.removeCallback(mMediaRouterCallback);
        }

        public MediaRouteSelector getMediaRouteSelector() {
            return mMediaRouteSelector;
        }


    }
    public static UIComponent getUiComponent() {
        if(sUIComponent == null) {
            throw new RuntimeException("Cannot get MediaRouteManager UIComponent without first creating an instance of MediaRouteManager");
        }
        return sUIComponent;
    }

    public interface MultiplexerControlInterface {

        void requestMediaRouteFocus();

        void notifyMediaRouteLost();
    }
    public MediaRouteManager (final Context context, final MultiplexerControlInterface callbacks) {
        mCallbacks = callbacks;
        mContext = context;
        mMediaRouter = MediaRouter.getInstance(context);

        sUIComponent = new UIComponent();

        this.animatorRunnable = new AnimatorRunnable(new AnimatorRunnable.TickerInterface() {
            @Override
            public void onTick() {
                updateProgress();
            }
        });

        mMediaRouterCallback = new MyMediaRouterCallback();

        mMediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(CastMediaControlIntent.categoryForCast(APPLICATION_ID))
                .build();

        mCastClientListener = new Cast.Listener() {
            @Override
            public void onApplicationStatusChanged() {
                if (mApiClient != null) {
                    Log.d(TAG, "onApplicationStatusChanged: "
                            + Cast.CastApi.getApplicationStatus(mApiClient));
                }
            }

            @Override
            public void onVolumeChanged() {
                if (mApiClient != null) {
                    Log.d(TAG, "onVolumeChanged: " + Cast.CastApi.getVolume(mApiClient));
                }
            }

            @Override
            public void onApplicationDisconnected(int errorCode) {
                teardown();
            }
        };

        mRemoteMediaPlayer = new RemoteMediaPlayer();

        mRemoteMediaPlayer.setOnStatusUpdatedListener(
                statusUpdatedListener);
        mRemoteMediaPlayer.setOnMetadataUpdatedListener(
                new RemoteMediaPlayer.OnMetadataUpdatedListener() {
                    @Override
                    public void onMetadataUpdated() {
                        currentMediaInfo = mRemoteMediaPlayer.getMediaInfo();
                        if (currentMediaInfo != null) {
                            MediaMetadata metadata = currentMediaInfo.getMetadata();
                        }
                    }
                });
    }

    private void updateProgress() {
        if (mRemoteMediaPlayer != null && currentMediaInfo != null) {
                if (parentCallbacks != null) {
                    parentCallbacks.onProgress(mRemoteMediaPlayer.getApproximateStreamPosition() / (float) mRemoteMediaPlayer.getStreamDuration());
                }
        } else if (parentCallbacks != null) {
            parentCallbacks.onProgress(0);
        }
    }

    final ResultCallback<RemoteMediaPlayer.MediaChannelResult> statusResultCallback = new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
        @Override
        public void onResult(@NonNull RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
            statusUpdatedListener.onStatusUpdated();
        }
    };

    final RemoteMediaPlayer.OnStatusUpdatedListener statusUpdatedListener = new RemoteMediaPlayer.OnStatusUpdatedListener() {
        @Override
        public void onStatusUpdated() {
            MediaStatus mediaStatus = mRemoteMediaPlayer.getMediaStatus();
            if (mediaStatus != null) {
                switch (mediaStatus.getPlayerState()) {
                    case MediaStatus.PLAYER_STATE_PLAYING:
                        animatorRunnable.start();
                        break;
                    case MediaStatus.PLAYER_STATE_PAUSED:
                        animatorRunnable.pause();
                        break;
                    case MediaStatus.PLAYER_STATE_IDLE:
                        switch(mediaStatus.getIdleReason()){
                            case MediaStatus.IDLE_REASON_FINISHED:
                                next();
                                break;
                        }
                        break;
                }
            }

            if(mWaitingToLoad) {
                loadCurrentTrack();
            }

            //try to seek
            if (mRequestedSeekPosition > 0) {
                setPlaybackPosition(mRequestedSeekPosition);
            }
        }
    };

    final ResultCallback<RemoteMediaPlayer.MediaChannelResult> autoPlayChangeCallback = new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
        @Override
        public void onResult(RemoteMediaPlayer.MediaChannelResult result) {
            Status status = result.getStatus();
            if (!status.isSuccess()) {
                Log.w(TAG, "Unable to toggle pause: "
                        + status.getStatusCode());
            }
        }
    };

    @Override
    public void init(Context context, @NonNull AudioEngineCallbacks callbacks) {
        this.parentCallbacks = callbacks;
    }

    @Override
    public void play() {
        if (mRemoteMediaPlayer != null && mApiClient != null && mApplicationStarted && currentMediaInfo != null) {
            mRemoteMediaPlayer.play(mApiClient).setResultCallback(autoPlayChangeCallback);
        }
        mAutoPlay = true;
        parentCallbacks.onAutoPlayStateChange(mAutoPlay);
    }

    @Override
    public void pause() {
        if (mRemoteMediaPlayer != null && mApiClient != null && mApplicationStarted && currentMediaInfo != null) {
            mRemoteMediaPlayer.pause(mApiClient).setResultCallback(autoPlayChangeCallback);
        }
        mAutoPlay = false;
        parentCallbacks.onAutoPlayStateChange(mAutoPlay);
    }

    @Override
    public void next() {
        trackProvider.incrementTrackIndex();
        loadCurrentTrack();
    }

    @Override
    public void previous() {
        trackProvider.decrementTrackIndex();
        loadCurrentTrack();
    }

    @Override
    public boolean willAutoPlay() {
        return mAutoPlay;
    }

    @Override
    public void reset() {
        teardown();
    }

    @Override
    public void setTrackProvider(TrackProvider trackProvider) {
        if(this.trackProvider != null) {
            this.trackProvider.dettachListener(this);
        }

        this.trackProvider = trackProvider;

        if (trackProvider != null) {
            this.trackProvider.attachListener(this);
            loadCurrentTrack();
        }
    }

    private void loadCurrentTrack() {
        if (mRemoteMediaPlayer != null && mApiClient != null && mApplicationStarted) {
            mWaitingToLoad = false;

            trackProvider.cancelAllTrackRequests();
            trackProvider.requestNthTrack(trackProvider.getCurrentTrackIndex(), new TrackProvider.TrackCallback() {
                @Override
                public void onTrackRetrieved(AudioTrack track) {
                    parentCallbacks.onTrackChange(track);
                    MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
                    mediaMetadata.addImage(new WebImage(Uri.parse(track.getArtworkUrl()), 200, 200));
                    mediaMetadata.putString(MediaMetadata.KEY_TITLE, track.getName());
                    mediaMetadata.putString(MediaMetadata.KEY_ARTIST, track.getArtist());

                    final int seekPos = mRequestedSeekPosition;
                    mRequestedSeekPosition = 0;

                    String mimeType = track.getUrl().contains(".mp4") ? "audio/mp4" : "audio/mpeg";

                    MediaInfo mediaInfo = new MediaInfo.Builder(
                            track.getUrl())
                            .setContentType(mimeType)
                            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                            .setMetadata(mediaMetadata)
                            .build();
                    try {
                        mRemoteMediaPlayer.load(mApiClient, mediaInfo, mAutoPlay, seekPos).setResultCallback(statusResultCallback);
                    } catch (IllegalStateException e) {
                        Log.e(TAG, "Problem occurred with media during loading", e);
                    } catch (Exception e) {
                        Log.e(TAG, "Problem opening media during loading", e);
                    }
                }

                @Override
                public void onError(String errorMsg) {

                }
            });


        } else {
            mWaitingToLoad = true;
        }
    }

    @Override
    public void onTracksInvalidated() {
        if (trackProvider != null && trackProvider.getTrackCount() > 0) {
            loadCurrentTrack();
        } else {
            parentCallbacks.onTrackChange(null);
        }
    }

    @Override
    public void setVolume(float volume) {
        if (mRemoteMediaPlayer != null && mApiClient != null && mApplicationStarted && currentMediaInfo != null) {
            mRemoteMediaPlayer.setStreamVolume(mApiClient, volume).setResultCallback(autoPlayChangeCallback);
        }
    }

    @Override
    public int getPlaybackPosition() {
        return (int) mRemoteMediaPlayer.getApproximateStreamPosition();
    }

    @Override
    public void setPlaybackPosition(int position) {
        if (mRemoteMediaPlayer != null && mApiClient != null && mApplicationStarted && currentMediaInfo != null) {
            mRequestedSeekPosition = 0;
            mRemoteMediaPlayer.seek(mApiClient, position).setResultCallback(autoPlayChangeCallback);
        } else {
            mRequestedSeekPosition = position;
        }
    }


    private class MyMediaRouterCallback extends MediaRouter.Callback {

        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {

            mCallbacks.requestMediaRouteFocus();

            mSelectedDevice = CastDevice.getFromBundle(info.getExtras());
            String routeId = info.getId();

            Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
                    .builder(mSelectedDevice, mCastClientListener);

            mApiClient = new GoogleApiClient.Builder(mContext)
                    .addApi(Cast.API, apiOptionsBuilder.build())
                    .addConnectionCallbacks(new ConnectionCallbacks())
                    .addOnConnectionFailedListener(new ConnectionFailedListener())
                    .build();

            mApiClient.connect();
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
            teardown();
            mSelectedDevice = null;
        }
    }



    private class ConnectionCallbacks implements
            GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnected(Bundle connectionHint) {
            if (mWaitingForReconnect) {
                mWaitingForReconnect = false;
//                reconnectChannels();
            } else {
                try {
                    Cast.CastApi.launchApplication(mApiClient, APPLICATION_ID, false)
                            .setResultCallback(
                                    new ResultCallback<Cast.ApplicationConnectionResult>() {
                                        @Override
                                        public void onResult(Cast.ApplicationConnectionResult result) {
                                            Status status = result.getStatus();
                                            if (status.isSuccess()) {
                                                mApplicationStarted = true;
                                                ApplicationMetadata applicationMetadata =
                                                        result.getApplicationMetadata();
                                                mSessionId = result.getSessionId();
                                                String applicationStatus = result.getApplicationStatus();
                                                boolean wasLaunched = result.getWasLaunched();

                                                try {
                                                    Cast.CastApi.setMessageReceivedCallbacks(mApiClient,
                                                            mRemoteMediaPlayer.getNamespace(), mRemoteMediaPlayer);
                                                } catch (IOException e) {
                                                    Log.e(TAG, "Exception while creating media channel", e);
                                                }

                                                mRemoteMediaPlayer.requestStatus(mApiClient).setResultCallback(statusResultCallback);

                                            } else {
                                                teardown();
                                            }
                                        }
                                    });

                } catch (Exception e) {
                    Log.e(TAG, "Failed to launch application", e);
                }
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
            mWaitingForReconnect = true;
        }
    }

    private class ConnectionFailedListener implements
            GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(ConnectionResult result) {
            teardown();
        }
    }

    private void teardown() {
        Log.d(TAG, "teardown");
        if (mApiClient != null) {
            if (mApplicationStarted) {
                if (mApiClient.isConnected() || mApiClient.isConnecting()) {
//                    try {
                        Cast.CastApi.stopApplication(mApiClient, mSessionId);
//                        if (mHelloWorldChannel != null) {
//                            Cast.CastApi.removeMessageReceivedCallbacks(
//                                    mApiClient,
//                                    mHelloWorldChannel.getNamespace());
//                            mHelloWorldChannel = null;
//                        }
//                    } catch (IOException e) {
//                        Log.e(TAG, "Exception while removing channel", e);
//                    }
                    mApiClient.disconnect();
                }
                mApplicationStarted = false;
            }
            mApiClient = null;
        }
        mSelectedDevice = null;
        mWaitingForReconnect = false;
        mSessionId = null;

        mCallbacks.notifyMediaRouteLost();
    }


}
