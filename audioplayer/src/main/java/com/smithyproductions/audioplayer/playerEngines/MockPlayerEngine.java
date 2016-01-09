package com.smithyproductions.audioplayer.playerEngines;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.Log;

import com.smithyproductions.audioplayer.AudioTrack;
import com.smithyproductions.audioplayer.interfaces.MediaPlayerCallbacks;


/**
 * Created by rory on 07/01/16.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class MockPlayerEngine extends BasePlayerEngine {

    private static final int MOCK_DURATION = 1000 * 20;

    private final AnimatorRunnable fakePlayerAnimator;
    private float currentProgress;
    private AudioTrack currentTrack;

    enum State {IDLE, PLAYING, PAUSED, FINISHED;}

    private @Nullable MediaPlayerCallbacks callbacks;

    private State currentState = State.IDLE;

    private boolean playWhenReady;

    AnimatorRunnable.TickerInterface tickerInterface = new AnimatorRunnable.TickerInterface() {
        @Override
        public void onTick() {
            if(callbacks != null) {
                callbacks.onProgress(currentProgress);
            }
            currentProgress += AnimatorRunnable.Time_INCR / (float) MOCK_DURATION;

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
    public boolean isFinished() {
        return currentState == State.FINISHED;
    }

    @Override
    public void unloadCurrent() {
        fakePlayerAnimator.pause();
        this.currentTrack = null;
    }

    @Override
    public AudioTrack getTrack() {
        return currentTrack;
    }

    @Override
    public void seekTo(int position) {
        //todo actually implement seekTo rather than seekTo(0)
        fakePlayerAnimator.reset();
    }

    @Override
    public int getDuration() {
        return MOCK_DURATION;
    }

    @Override
    public boolean isPreparing() {
        return currentState == State.IDLE;
    }

    @Override
    public void setVolume(float volume) {
        //noop
        Log.d("MockPlayer", "setting volume to "+volume);
    }

    @Override
    public float getProgress() {
        return currentProgress;
    }

    public void onAnimationEnd() {
        Log.d("MockPlayer", "finished playing: " + currentTrack);
        currentState = State.FINISHED;

        if(callbacks != null) {
            callbacks.onTrackFinished();
        }
    }

}