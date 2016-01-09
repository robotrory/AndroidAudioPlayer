package com.smithyproductions.audioplayer.playerEngines;

import android.os.Handler;

/**
 * Created by rory on 09/01/16.
 */
public class AnimatorRunnable implements Runnable {
    protected final Handler handler;
    private final TickerInterface tickerInterface;
    boolean playing;

    public static final int Time_INCR = 100;
    private Runnable tickerRunnable = new Runnable() {
        @Override
        public void run() {
            tickerInterface.onTick();
        }
    };


    public interface TickerInterface {
        void onTick();
    }

    public AnimatorRunnable(final TickerInterface tickerInterface) {
        this.tickerInterface = tickerInterface;
        handler = new Handler();
    }

    public void start() {
        playing = true;
        handler.post(this);
    }

    public void pause() {
        playing = false;
    }

    public void reset() {
        playing = false;
        handler.removeCallbacks(this);
    }

    @Override
    public void run() {
        if (playing) {
            handler.post(tickerRunnable);
            handler.removeCallbacks(this);
            handler.postDelayed(this, Time_INCR);
        }
    }
};
