package com.smithyproductions.audioplayer.playerEngines;

import android.media.AudioManager;
import android.media.MediaPlayer;

import com.smithyproductions.audioplayer.AudioTrack;
import com.smithyproductions.audioplayer.interfaces.MediaPlayerCallbacks;

import java.io.IOException;


/**
 * Created by rory on 07/01/16.
 */
public class MediaPlayerEngine extends BasePlayerEngine implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {

    enum State {IDLE, PREPARING, PLAYING, PAUSED, FINISHED;}

    private final MediaPlayer mediaPlayer;
    private MediaPlayerCallbacks callbacks;

    private State currentState = State.IDLE;
    private boolean playWhenReady;

    private final AnimatorRunnable animatorRunnable;

    public MediaPlayerEngine() {
        this.mediaPlayer = new MediaPlayer();
        this.mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        this.mediaPlayer.setOnPreparedListener(this);
        this.mediaPlayer.setOnCompletionListener(this);

        this.animatorRunnable = new AnimatorRunnable(new AnimatorRunnable.TickerInterface() {
            @Override
            public void onTick() {
                callbacks.onProgress(mediaPlayer.getCurrentPosition() / (float) mediaPlayer.getDuration());
            }
        });

    }

    @Override
    public void play() {
        playWhenReady = true;
        if(!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            animatorRunnable.start();
        }
        currentState = State.PLAYING;
    }

    @Override
    public void pause() {
        playWhenReady = false;
        if(mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            animatorRunnable.pause();
        }
        currentState = State.PAUSED;
    }

    @Override
    public void loadTrack(AudioTrack track) {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(track.getUrl());
            mediaPlayer.prepareAsync();
            currentState = State.PREPARING;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setCallbackHandler(MediaPlayerCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    @Override
    public boolean isLoaded() {
        return currentState != State.IDLE && currentState != State.PREPARING;
    }

    @Override
    public boolean isFinished() {
        return currentState == State.FINISHED;
    }

    @Override
    public void unloadCurrent() {
        if(isLoaded()) {
            mediaPlayer.stop();
        }
        animatorRunnable.reset();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (playWhenReady) {
            play();
        } else {
            pause();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        currentState = State.FINISHED;
        callbacks.onTrackFinished();
    }
}
