package com.smithyproductions.audioplayer.audioEngines;

import com.smithyproductions.audioplayer.interfaces.AudioEngineCallbacks;
import com.smithyproductions.audioplayer.interfaces.MediaPlayerCallbacks;
import com.smithyproductions.audioplayer.playerEngines.BasePlayerEngine;
import com.smithyproductions.audioplayer.trackProviders.TrackProvider;

/**
 * Created by rory on 07/01/16.
 */
public abstract class BaseAudioEngine implements MediaPlayerCallbacks {

    public abstract void play();
    public abstract void pause();
    public abstract void next();
    public abstract void previous();

    public abstract void init(Class<? extends BasePlayerEngine> mediaPlayerClass, TrackProvider trackProvider, AudioEngineCallbacks callbacks);

}
