package com.smithyproductions.audioplayer.playerEngines;

import com.smithyproductions.audioplayer.AudioTrack;
import com.smithyproductions.audioplayer.interfaces.MediaPlayerCallbacks;

/**
 * Created by rory on 07/01/16.
 */
public abstract class BasePlayerEngine {
    public abstract void play();
    public abstract void pause();
    public abstract void loadTrack(final AudioTrack track);
    public abstract void setCallbackHandler(MediaPlayerCallbacks callbacks);

    public abstract boolean isLoaded();
    public abstract boolean isFinished();

    public abstract void unloadCurrent();
}
