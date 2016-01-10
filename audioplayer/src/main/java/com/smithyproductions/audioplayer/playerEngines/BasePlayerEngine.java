package com.smithyproductions.audioplayer.playerEngines;

import android.content.Context;
import android.support.annotation.Nullable;

import com.smithyproductions.audioplayer.AudioTrack;
import com.smithyproductions.audioplayer.interfaces.MediaPlayerCallbacks;

/**
 * Created by rory on 07/01/16.
 */
public abstract class BasePlayerEngine {
    protected final Context context;

    public abstract void play();
    public abstract void pause();
    public abstract void loadTrack(final AudioTrack track);
    public abstract void setCallbackHandler(@Nullable MediaPlayerCallbacks callbacks);

    public abstract boolean isFinished();

    public abstract void unloadCurrent();

    public abstract AudioTrack getTrack();

    public abstract void seekTo(int position);

    public abstract int getDuration();

    public abstract boolean isPreparing();

    public abstract void setVolume(float volume);

    public abstract float getProgress();

    public abstract boolean willAutoPlay();

    @Nullable
    protected MediaPlayerCallbacks callbacks;
    protected boolean playWhenReady;

    protected void setPlayWhenReady(boolean playWhenReady) {
        this.playWhenReady = playWhenReady;
    }

    public BasePlayerEngine (final Context context) {
        this.context = context;
    }
}
