package com.smithyproductions.audioplayer.MediaRouter;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
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
import com.smithyproductions.audioplayer.interfaces.ControlInterface;

import java.io.IOException;

/**
 * Created by rory on 25/01/16.
 */
public class MediaRouteManager implements ControlInterface {

    private static final String TAG = "MediaRouteManager";
    public static final String APPLICATION_ID = "CC81EAFB";
    private final MediaRouter mMediaRouter;
    private final MediaRouteSelector mMediaRouteSelector;
    private final MyMediaRouterCallback mMediaRouterCallback;
    private final Context mContext;
    private final Cast.Listener mCastClientListener;
    private final RemoteMediaPlayer mRemoteMediaPlayer;
    private boolean enabled;
    private CastDevice mSelectedDevice;
    private GoogleApiClient mApiClient;
    private boolean mApplicationStarted;
    private boolean mWaitingForReconnect;
    private String mSessionId;

    public MediaRouteManager (final Context context) {
        mContext = context;
        mMediaRouter = MediaRouter.getInstance(context);

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
                new RemoteMediaPlayer.OnStatusUpdatedListener() {
                    @Override
                    public void onStatusUpdated() {
                        MediaStatus mediaStatus = mRemoteMediaPlayer.getMediaStatus();
                        if (mediaStatus != null) {
                            boolean isPlaying = mediaStatus.getPlayerState() ==
                                    MediaStatus.PLAYER_STATE_PLAYING;
                        }
                    }
                });

        mRemoteMediaPlayer.setOnMetadataUpdatedListener(
                new RemoteMediaPlayer.OnMetadataUpdatedListener() {
                    @Override
                    public void onMetadataUpdated() {
                        MediaInfo mediaInfo = mRemoteMediaPlayer.getMediaInfo();
                        if (mediaInfo != null) {
                            MediaMetadata metadata = mediaInfo.getMetadata();
                        }
                    }
                });
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public MediaRouteSelector getMediaRouteSelector() {
        return mMediaRouteSelector;
    }

    @Override
    public void onAutoPlayChange(boolean autoplay) {
        if (mRemoteMediaPlayer != null && mApiClient != null && mApplicationStarted) {
            if (autoplay) {
                mRemoteMediaPlayer.play(mApiClient).setResultCallback(
                        new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                            @Override
                            public void onResult(RemoteMediaPlayer.MediaChannelResult result) {
                                Status status = result.getStatus();
                                if (!status.isSuccess()) {
                                    Log.w(TAG, "Unable to toggle pause: "
                                            + status.getStatusCode());
                                }
                            }
                        });
            } else {
                mRemoteMediaPlayer.pause(mApiClient).setResultCallback(
                        new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                            @Override
                            public void onResult(RemoteMediaPlayer.MediaChannelResult result) {
                                Status status = result.getStatus();
                                if (!status.isSuccess()) {
                                    Log.w(TAG, "Unable to toggle pause: "
                                            + status.getStatusCode());
                                }
                            }
                        });
            }
        }
    }

    @Override
    public void onTrackChange(@Nullable AudioTrack track) {
        if (track != null && mRemoteMediaPlayer != null && mApiClient != null && mApplicationStarted) {
            MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
            mediaMetadata.addImage(new WebImage(Uri.parse(track.getArtworkUrl()), 200, 200));
            mediaMetadata.putString(MediaMetadata.KEY_TITLE, track.getName());
            mediaMetadata.putString(MediaMetadata.KEY_ARTIST, track.getArtist());

            String mimeType = track.getUrl().contains(".mp4") ? "audio/mp4" : "audio/mpeg";

            MediaInfo mediaInfo = new MediaInfo.Builder(
                    track.getUrl())
                    .setContentType(mimeType)
                    .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                    .setMetadata(mediaMetadata)
                    .build();
            try {
                mRemoteMediaPlayer.load(mApiClient, mediaInfo, true)
                        .setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                            @Override
                            public void onResult(RemoteMediaPlayer.MediaChannelResult result) {
                                if (result.getStatus().isSuccess()) {
                                    Log.d(TAG, "Media loaded successfully");
                                }
                            }
                        });
            } catch (IllegalStateException e) {
                Log.e(TAG, "Problem occurred with media during loading", e);
            } catch (Exception e) {
                Log.e(TAG, "Problem opening media during loading", e);
            }
        }
    }

    @Override
    public void onProgressChange(float progress) {

    }

    @Override
    public void onDataChange(boolean hasData) {

    }

    @Override
    public void setTurntable(Turntable turntable) {

    }


    private class MyMediaRouterCallback extends MediaRouter.Callback {

        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
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
                                                mRemoteMediaPlayer
                                                        .requestStatus(mApiClient)
                                                        .setResultCallback(
                                                                new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                                                                    @Override
                                                                    public void onResult(RemoteMediaPlayer.MediaChannelResult result) {
                                                                        if (!result.getStatus().isSuccess()) {
                                                                            Log.e(TAG, "Failed to request status.");
                                                                        }
                                                                    }
                                                                });

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
    }

    public void beginScan() {
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
    }

    public void endScan() {
        mMediaRouter.removeCallback(mMediaRouterCallback);
    }
}
