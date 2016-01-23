package com.smithyproductions.audioplayer.audioEngines;

import android.content.Context;
import android.support.annotation.Nullable;

import com.smithyproductions.audioplayer.interfaces.AudioEngineCallbacks;
import com.smithyproductions.audioplayer.interfaces.MediaPlayerCallbacks;
import com.smithyproductions.audioplayer.playerEngines.BasePlayerEngine;
import com.smithyproductions.audioplayer.trackProviders.TrackProvider;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by rory on 07/01/16.
 */
public abstract class BaseAudioEngine implements MediaPlayerCallbacks, TrackProvider.TrackProviderListener {

    public abstract void play();

    public abstract void pause();

    public abstract void next();

    public abstract void previous();

    protected AudioEngineCallbacks parentCallbacks;

    private boolean autoPlay;

    public abstract void init(Class<? extends BasePlayerEngine> mediaPlayerClass, final Context context, AudioEngineCallbacks callbacks);

    public void setTrackProvider(@Nullable TrackProvider trackProvider) {
        if (this.trackProvider != null) {
            this.trackProvider.dettachListener(this);
        }

        this.trackProvider = trackProvider;

        if (this.trackProvider != null) {
            this.trackProvider.attachListener(this);

            //here we try to load any track we can get our hands on, regardless of whether it'll be played or not
            this.onTracksInvalidated();
        }
    }

    public abstract void reset();

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

    public boolean willAutoPlay() {
        return autoPlay;
    }

    public abstract void setVolume(float volume);
}
