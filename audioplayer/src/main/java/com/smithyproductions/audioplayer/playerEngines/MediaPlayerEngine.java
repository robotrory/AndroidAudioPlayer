package com.smithyproductions.audioplayer.playerEngines;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.util.Log;

import com.smithyproductions.audioplayer.AudioTrack;
import com.smithyproductions.audioplayer.PlayerKicker;
import com.smithyproductions.audioplayer.interfaces.MediaPlayerCallbacks;

import java.io.IOException;

/**
 * Created by rory on 07/01/16.
 */
public class MediaPlayerEngine extends BasePlayerEngine implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, PlayerKicker.KickerInterface, AudioManager.OnAudioFocusChangeListener {

    private final WifiManager.WifiLock mWifiLock;
    private final PlayerKicker audioKicker;
    private final AudioManager audioManager;
    private AudioTrack track;
    private int trackDuration = -1;
    private float currentProgress = 0;
    private float duckVolumeMultiplier = 1.0f;
    private float globalVolume = 1.0f;

    enum State {IDLE, PREPARED, PREPARING, PLAYING, PAUSED, FINISHED;}
    enum FocusState {FULL_FOCUS, HALF_FOCUS, NO_FOCUS}

    private MediaPlayer mediaPlayer;

    private State currentState = State.IDLE;
    private FocusState currentFocusState = FocusState.NO_FOCUS;

    private final AnimatorRunnable animatorRunnable;

    public MediaPlayerEngine(final Context context) {
        super(context);

        this.audioKicker = PlayerKicker.obtainKicker(context);
        this.animatorRunnable = new AnimatorRunnable(new AnimatorRunnable.TickerInterface() {
            @Override
            public void onTick() {
                if ((currentState == State.PLAYING || currentState == State.PAUSED) && trackDuration >= 0) {
                    currentProgress = mediaPlayer.getCurrentPosition() / (float) trackDuration;
                    if (callbacks != null) {
                        callbacks.onProgress(currentProgress);
                    }
                } else if (callbacks != null) {
                    callbacks.onProgress(0);
                }
            }
        });

        mWifiLock = ((WifiManager) context.getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "smithLock");

        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                switch (currentFocusState) {
                    case NO_FOCUS:
                        //unpause
                        if(currentState == State.PLAYING) {
                            mediaPlayer.start();
                            animatorRunnable.start();
                        }
                    case HALF_FOCUS:
                        //restore volume
                        duckVolumeMultiplier = 1.0f;
                        updateVolume();
                    case FULL_FOCUS:
                        break;
                }
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if(currentState == State.PLAYING) {
                    mediaPlayer.pause();
                    animatorRunnable.pause();
                }
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if(currentState == State.PLAYING) {
                    mediaPlayer.pause();
                    animatorRunnable.pause();
                }
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                duckVolumeMultiplier = 0.1f;
                updateVolume();

                break;
        }
    }

    private boolean requestFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN);
    }

    private boolean abandonFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                audioManager.abandonAudioFocus(this);
    }

    private MediaPlayer createMediaPlayer(Context context) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
        return mediaPlayer;
    }

    @Override
    public void play() {
        setPlayWhenReady(true);
        if(currentState == State.PREPARED || currentState == State.PAUSED) {
            Log.d("MediaPlayerEngine", "playing: "+track);
            mediaPlayer.start();
            animatorRunnable.start();
            currentState = State.PLAYING;
        } else {
            Log.d("MediaPlayerEngine", "not in correct state ("+currentState+") to play: "+track);
        }

    }

    @Override
    public void pause() {
        setPlayWhenReady(false);
        if(currentState == State.PREPARED || currentState == State.PLAYING) {
            Log.d("MediaPlayerEngine", "pausing: " + track);
            if(currentState == State.PLAYING) {
                mediaPlayer.pause();
                animatorRunnable.pause();
            }
            currentState = State.PAUSED;
        } else {
            Log.d("MediaPlayerEngine", "not in correct state ("+currentState+") to pause: "+track);
        }

    }

    @Override
    public void loadTrack(final AudioTrack track) {
        try {
            if(!mWifiLock.isHeld()) {
                mWifiLock.acquire();
            }

            requestFocus();

            if(mediaPlayer == null) {
                mediaPlayer = createMediaPlayer(context);
            }

            this.track = track;
            mediaPlayer.reset();
            mediaPlayer.setDataSource(track.getUrl());

            audioKicker.notifyPrepareStart(track, this);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mediaPlayer.prepare();
                    } catch (final IllegalStateException e) {
                        e.printStackTrace();
                        Log.e("MediaPlayerEngine", "error preparing: " + track);
                    } catch (final IOException e) {
                        e.printStackTrace();
                        Log.e("MediaPlayerEngine", "error preparing: " + track);
                    } catch (final Exception e) {
                        e.printStackTrace();
                        Log.e("MediaPlayerEngine", "error preparing: " + track);
                    }

//                    onPrepared(mediaPlayer);
                }
            }).start();
//            mediaPlayer.prepareAsync();
            currentState = State.PREPARING;
            Log.d("MediaPlayerEngine", "preparing: "+track);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setCallbackHandler(MediaPlayerCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    @Override
    public boolean isFinished() {
        return currentState == State.FINISHED;
    }

    @Override
    public void unloadCurrent() {
        if(currentState == State.PAUSED || currentState == State.PLAYING) {
            mediaPlayer.stop();
        }

        releaseMediaPlayer();
        currentState = State.IDLE;
        trackDuration = -1;
        currentProgress = 0;
        track = null;
        animatorRunnable.reset();

        abandonFocus();

        if(mWifiLock.isHeld()) {
            mWifiLock.release();
        }
    }

    private void releaseMediaPlayer() {
        if(mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public AudioTrack getTrack() {
        return track;
    }

    @Override
    public void seekTo(int position) {
        if(currentState == State.PLAYING || currentState == State.PAUSED) {
            mediaPlayer.seekTo(position);
        }
    }

    @Override
    public int getDuration() {
        return trackDuration;
    }

    @Override
    public boolean isPreparing() {
        return currentState == State.PREPARING || currentState == State.IDLE;
    }

    @Override
    public void setVolume(float volume) {
        this.globalVolume = volume;
        updateVolume();
    }

    private void updateVolume() {
        if(mediaPlayer != null) {
            final int correctedVolume = (int) (globalVolume * duckVolumeMultiplier);
            mediaPlayer.setVolume(correctedVolume, correctedVolume);
        }
    }

    @Override
    public float getProgress() {
        return currentProgress;
    }

    @Override
    public boolean willAutoPlay() {
        return playWhenReady;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        currentState = State.PREPARED;
        audioKicker.notifyPrepareEnd(track);
        Log.d("MediaPlayerEngine", "prepared: "+track);
        trackDuration = mp.getDuration();
        if (playWhenReady) {
            play();
        } else {
            pause();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        currentState = State.FINISHED;
        if(callbacks != null) {
            callbacks.onTrackFinished();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d("MediaPlayerEngine", "what: "+what+" extra: "+extra+" error: "+track);
        return false;
    }

    @Override
    public void onPlayerShouldHaveLoaded(AudioTrack audioTrack, boolean urlValid) {
        if(track != null && track.equals(audioTrack)) {
            if(currentState == State.PREPARING) {
                Log.d("MediaPlayerEngine", "player kicker is requesting us to reload");
                if(urlValid) {
                    releaseMediaPlayer();
                    currentState = State.IDLE;
                    loadTrack(track);
                } else {
                    Log.e("MediaPlayerEngine", "url not valid, HELP!");

                    if(callbacks != null) {
                        callbacks.onGeneralError();
                    }
                }
            } else {
                Log.d("MediaPlayerEngine", "disregarding player kicker request because we're not preparing");
            }
        }
    }

}
