package com.smithyproductions.audioplayer.playerEngines;

import android.support.annotation.Nullable;

import com.smithyproductions.audioplayer.AudioTrack;
import com.smithyproductions.audioplayer.interfaces.MediaPlayerCallbacks;

/**
 * Created by rory on 07/01/16.
 */
public abstract class BasePlayerEngine {
    public abstract void play();
    public abstract void pause();
    public abstract void loadTrack(final AudioTrack track);
    public abstract void setCallbackHandler(@Nullable MediaPlayerCallbacks callbacks);

    public abstract boolean isFinished();

    public abstract void unloadCurrent();

    public abstract AudioTrack getTrack();

    public abstract void seekTo(int position);

}
