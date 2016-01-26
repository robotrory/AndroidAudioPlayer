package com.smithyproductions.audioplayer.interfaces;

import android.content.Context;
import android.support.annotation.NonNull;

import com.smithyproductions.audioplayer.playerEngines.BasePlayerEngine;
import com.smithyproductions.audioplayer.trackProviders.TrackProvider;

/**
 * Created by rory on 26/01/16.
 */
public interface AudioEngineInterface {
    void init(final Context context, @NonNull AudioEngineCallbacks callbacks);

    void play();

    void pause();

    void next();

    void previous();

    boolean willAutoPlay();

    void reset();

    void setTrackProvider(TrackProvider trackProvider);

    void setVolume(float volume);

    int getPlaybackPosition();

    void setPlaybackPosition(int position);
}
