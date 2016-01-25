package com.smithyproductions.audioplayer.controls;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;

import com.smithyproductions.audioplayer.Turntable;
import com.smithyproductions.audioplayer.AudioTrack;

/**
 * Created by rory on 10/01/16.
 */
public class MediaSessionControl extends ControlAdapter implements BitmapLoaderControl.BitmapLoaderInterface {
    public static final long CAPABILITIES = PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls mTransportController;
    private BitmapLoaderControl bitmapLoader;

    public MediaSessionControl () {
        bitmapLoader = BitmapLoaderControl.getInstance();
        bitmapLoader.attachBitmapLoaderInterface(this);
    }

    @Override
    public void setTurntable(Turntable turntable) {
        super.setTurntable(turntable);
        if (turntable != null) {
            turntable.attachControl(bitmapLoader);
        }
    }

    private void setupMediaSession() {
    /* Activate Audio Manager */

        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_BUTTON);
        turntable.getService().registerReceiver(mediaButtonReceiver, filter);


        final Intent mMediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);

        final PendingIntent mMediaButtonPendingIntent = PendingIntent.getBroadcast(turntable.getService(), 0, mMediaButtonIntent, 0);


        ComponentName receiver = new ComponentName(turntable.getService().getPackageName(), MediaSessionControl.class.getName());
        mediaSession = new MediaSessionCompat(turntable.getService(), "PlayerService", receiver, null);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PAUSED, 0, 0)
                .setActions(CAPABILITIES)
                .build());
//        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
//                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.getArtist())
//                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "Test Album")
//                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.getName())
//                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 10000)
//                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
//                        BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
//                .build());

        mediaSession.setMediaButtonReceiver(mMediaButtonPendingIntent);

        mediaSession.setCallback(mMediaSessionCallback);

        mediaSession.setActive(true);
    }

    @Override
    public void onDataChange(boolean hasData) {
        if(audioPlayerAttached) {
            if (hasData) {
                if (mediaSession == null) {
                    setupMediaSession();
                }
            }

            if (mediaSession != null) {
                mediaSession.setActive(hasData);
            }
        }
    }

    private AudioManager.OnAudioFocusChangeListener mAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {

        }
    };

    private void updateMediaSessionMetaData(AudioTrack track) {
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
        builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.getArtist());
//        builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, getAlbumName());
        builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.getName());

        if(bitmapLoader != null && bitmapLoader.hasBitmapForTrack(track)) {
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmapLoader.getCurrentBitmap());
        }
//        builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getDuration());
//        builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, MusicUtils.getArtwork(this, getAlbumID(), true));
        mediaSession.setMetadata(builder.build());
    }

    @Override
    public void onTrackChange(@Nullable AudioTrack track) {
        if (audioPlayerAttached && track != null) {
            if (mediaSession != null) {
                updateMediaSessionMetaData(track);
            }
        }
    }

    @Override
    public void onCurrentAudioTrackBitmapReady() {
        if (audioPlayerAttached && turntable.getTrack() != null) {
            if (mediaSession != null) {
                updateMediaSessionMetaData(turntable.getTrack());
            }
        }
    }

    @Override
    public void onAutoPlayChange(boolean autoplay) {
        if (audioPlayerAttached && mediaSession != null) {
            if (autoplay) {
                mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                        .setActions(CAPABILITIES)
                        .build());
            } else {
                mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PAUSED, 0, 0.0f)
                        .setActions(CAPABILITIES)
                        .build());
            }
        }
    }

    BroadcastReceiver mediaButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_MEDIA_BUTTON.equals(action) && audioPlayerAttached) {

                final KeyEvent keyEvent = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);

                if (keyEvent != null && KeyEvent.ACTION_DOWN == keyEvent.getAction()) {
                    switch (keyEvent.getKeyCode()) {
                        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                            Log.d("MediaSessionControl", "play/pause requested");
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PLAY:
                            Log.d("MediaSessionControl", "play requested");
                            handlePlay();
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PAUSE:
                            Log.d("MediaSessionControl", "pause requested");
                            handlePause();
                            break;
                        case KeyEvent.KEYCODE_MEDIA_NEXT:
                            Log.d("MediaSessionControl", "next requested");
                            handleNext();
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                            Log.d("MediaSessionControl", "previous requested");
                            handlePrevious();
                            break;
                    }
                }

            }
        }
    };

    protected void handlePlay() {
        if (audioPlayerAttached) {
            turntable.play();
        }
    }

    protected void handlePause() {
        if (audioPlayerAttached) {
            turntable.pause();
        }
    }

    protected void handleNext() {
        if (audioPlayerAttached) {
            turntable.nextTrack();
        }
    }

    protected void handlePrevious() {
        if (audioPlayerAttached) {
            turntable.previousTrack();
        }
    }

    protected void handleStop() {
        if (audioPlayerAttached) {
            turntable.stop();
        }
    }

    private final MediaSessionCompat.Callback mMediaSessionCallback = new MediaSessionCompat.Callback() {

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            mediaButtonReceiver.onReceive(turntable.getService(), mediaButtonEvent);
            return true;
        }

        @Override
        public void onPlay() {
            super.onPlay();
            Log.d("MediaSessionControl", "play requested");
            handlePlay();
        }

        @Override
        public void onPause() {
            super.onPause();
            Log.d("MediaSessionControl", "pause requested");
            handlePause();
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            Log.d("MediaSessionControl", "next requested");
            handleNext();
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            Log.d("MediaSessionControl", "previous requested");
            handlePrevious();
        }

        @Override
        public void onSeekTo(long pos) {
            super.onSeekTo(pos);
        }

        @Override
        public void onStop() {
            super.onStop();
            Log.d("MediaSessionControl", "stop requested");
            handleStop();
        }
    };
}
