package com.smithyproductions.audioplayer.playerEngines;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

import com.smithyproductions.audioplayer.AudioTrack;
import com.smithyproductions.audioplayer.interfaces.MediaPlayerCallbacks;


/**
 * Created by rory on 07/01/16.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class MockPlayerEngine extends BasePlayerEngine {

    private static final long MOCK_DURATION = 1000 * 20;

    private final AnimatorRunnable fakePlayerAnimator;
    private float currentProgress;
    private AudioTrack currentTrack;

    enum State {IDLE, PREPARING, PLAYING, PAUSED, FINISHED;}

    private MediaPlayerCallbacks callbacks;

    private State currentState = State.IDLE;

    private boolean playWhenReady;

    AnimatorRunnable.TickerInterface tickerInterface = new AnimatorRunnable.TickerInterface() {
        @Override
        public void onTick() {
            currentProgress += AnimatorRunnable.Time_INCR / (float) MOCK_DURATION;
            callbacks.onProgress(currentProgress);
            if (currentProgress >= 1.0f) {
                pause();
                onAnimationEnd();
            }
        }
    };

    class AnimatorUpdaterRunnable extends AnimatorRunnable {


        public AnimatorUpdaterRunnable(TickerInterface tickerInterface) {
            super(tickerInterface);
        }

        @Override
        public void reset() {
            currentProgress = 0;
            super.reset();
        }
    }

    public MockPlayerEngine() {


        fakePlayerAnimator = new AnimatorUpdaterRunnable(tickerInterface);

    }

    @Override
    public void play() {
        Log.d("MockPlayer", "playing");
        playWhenReady = true;
        currentState = State.PLAYING;
        fakePlayerAnimator.start();
    }

    @Override
    public void pause() {
        Log.d("MockPlayer", "pausing");
        playWhenReady = false;
        currentState = State.PAUSED;
        fakePlayerAnimator.pause();
    }

    @Override
    public void loadTrack(AudioTrack track) {
        Log.d("MockPlayer", "loading url: " + track);
        this.currentTrack = track;
        fakePlayerAnimator.reset();
        if (playWhenReady) {
            play();
        } else {
            pause();
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
        fakePlayerAnimator.pause();
        this.currentTrack = null;
    }

    public void onAnimationEnd() {
        Log.d("MockPlayer", "finished playing: " + currentTrack);
        currentState = State.FINISHED;
        callbacks.onTrackFinished();
    }

}