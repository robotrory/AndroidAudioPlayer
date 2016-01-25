package com.smithyproductions.audioplayer.controls;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

/**
 * Created by rory on 22/01/16.
 */
public class AudioFocusControl extends ControlAdapter {

    private final AudioManager am;

    public AudioFocusControl(final Context context) {
        am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        // Request audio focus for playback
        int result = am.requestAudioFocus(afChangeListener,
                // Use the music stream.
                AudioManager.STREAM_MUSIC,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
//            am.registerMediaButtonEventReceiver(RemoteControlReceiver);
            // Start playback.
        }
    }

    @Override
    public void onAutoPlayChange(boolean autoplay) {
        if (autoplay) {
            pausedByAudioFocus = false;
            int result = am.requestAudioFocus(afChangeListener,
                    // Use the music stream.
                    AudioManager.STREAM_MUSIC,
                    // Request permanent focus.
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.d("AudioFocusControl", "Obtained audio focus");
            } else {
                Log.d("AudioFocusControl", "Could not obtain audio focus");
            }

        } else {
            am.abandonAudioFocus(afChangeListener);
        }
    }

    private boolean pausedByAudioFocus;
    AudioManager.OnAudioFocusChangeListener afChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                public void onAudioFocusChange(int focusChange) {
                    if (audioPlayerAttached) {
                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                            Log.d("AudioFocusControl", "lost audio focus transiently");
                            // Pause playback
                            turntable.pause();
                            pausedByAudioFocus = true;
                        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                            Log.d("AudioFocusControl", "gained audio focus");
                            // raise volume
                            turntable.setVolume(1.0f);
                            // Resume playback
                            if (pausedByAudioFocus) {
                                Log.d("AudioFocusControl", "was paused by audiofocus so we've resumed");
                                turntable.play();
                            }
                        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                            Log.d("AudioFocusControl", "completely lost audio focus");
//                        am.unregisterMediaButtonEventReceiver(RemoteControlReceiver);
                            am.abandonAudioFocus(afChangeListener);
                            // Stop playback
                            turntable.pause();
                        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                            // Lower the volume
                            Log.d("AudioFocusControl", "lost audio focus, but can duck");
                            turntable.setVolume(0.1f);
                        }
                    }
                }
            };

}
