package com.smithyproductions.audioplayer.audioEngines;

import android.content.Context;
import android.support.annotation.Nullable;

import com.smithyproductions.audioplayer.interfaces.AudioEngineCallbacks;
import com.smithyproductions.audioplayer.interfaces.AudioEngineInterface;
import com.smithyproductions.audioplayer.interfaces.MediaPlayerCallbacks;
import com.smithyproductions.audioplayer.playerEngines.BasePlayerEngine;
import com.smithyproductions.audioplayer.trackProviders.TrackProvider;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by rory on 07/01/16.
 */
public abstract class BaseAudioEngine implements AudioEngineInterface, MediaPlayerCallbacks, TrackProvider.TrackProviderListener {

    protected final Class<? extends BasePlayerEngine> mMediaPlayerClass;

    protected AudioEngineCallbacks parentCallbacks;

    private boolean autoPlay;

    public BaseAudioEngine (final Class<? extends BasePlayerEngine> mediaPlayerClass) {
        this.mMediaPlayerClass = mediaPlayerClass;
    }


    @Override
    public void setTrackProvider(@Nullable TrackProvider trackProvider) {
        if (this.trackProvider != null) {
            this.trackProvider.dettachListener(this);
        }

        this.trackProvider = trackProvider;

        if (this.trackProvider != null) {
            this.trackProvider.attachListener(this);
        }

        this.onTracksInvalidated();
    }

    @Nullable
    protected TrackProvider trackProvider;

    protected void setAutoPlay(final boolean autoPlay) {
        this.autoPlay = autoPlay;
        parentCallbacks.onAutoPlayStateChange(autoPlay);
    }

    protected BasePlayerEngine createBasePlayerEngine(Class<? extends BasePlayerEngine> mediaPlayerClass, Context context) {
        try {
            return mediaPlayerClass.getConstructor(Context.class).newInstance(context);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Could not build player");
    }

    @Override
    public boolean willAutoPlay() {
        return autoPlay;
    }

    @Override
    public abstract void setVolume(float volume);
}
