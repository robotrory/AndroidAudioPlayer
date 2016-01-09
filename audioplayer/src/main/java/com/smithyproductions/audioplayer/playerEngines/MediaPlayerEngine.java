package com.smithyproductions.audioplayer.playerEngines;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.annotation.Nullable;

import com.smithyproductions.audioplayer.AudioTrack;
import com.smithyproductions.audioplayer.interfaces.MediaPlayerCallbacks;

import java.io.IOException;


/**
 * Created by rory on 07/01/16.
 */
public class MediaPlayerEngine extends BasePlayerEngine implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {

    private AudioTrack track;
    private float trackDuration = -1;

    enum State {IDLE, PREPARED, PREPARING, PLAYING, PAUSED, FINISHED;}

    private final MediaPlayer mediaPlayer;
    private @Nullable MediaPlayerCallbacks callbacks;

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
                if(callbacks != null) {
                    if ((currentState == State.PLAYING || currentState == State.PAUSED) && trackDuration >= 0) {
                        callbacks.onProgress(mediaPlayer.getCurrentPosition() / trackDuration);
                    } else {
                        callbacks.onProgress(0);
                    }
                }
            }
        });

    }

    @Override
    public void play() {
        playWhenReady = true;
        if(currentState == State.PREPARED || currentState == State.PAUSED) {
            mediaPlayer.start();
            animatorRunnable.start();
        }
        currentState = State.PLAYING;
    }

    @Override
    public void pause() {
        playWhenReady = false;
        if(currentState == State.PLAYING) {
            mediaPlayer.pause();
            animatorRunnable.pause();
        }
        currentState = State.PAUSED;
    }

    @Override
    public void loadTrack(AudioTrack track) {
        try {
            this.track = track;
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
    public boolean isFinished() {
        return currentState == State.FINISHED;
    }

    @Override
    public void unloadCurrent() {
        if(currentState == State.PAUSED || currentState == State.PLAYING) {
            mediaPlayer.stop();
        }
        currentState = State.IDLE;
        trackDuration = -1;
        animatorRunnable.reset();
    }

    @Override
    public AudioTrack getTrack() {
        return track;
    }

    @Override
    public void seekTo(int position) {
        mediaPlayer.seekTo(position);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        currentState = State.PREPARED;
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
}
