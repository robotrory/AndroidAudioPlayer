package com.smithyproductions.audioplayer.interfaces;

import android.support.annotation.Nullable;

import com.smithyproductions.audioplayer.Turntable;
import com.smithyproductions.audioplayer.AudioTrack;

/**
 * Created by rory on 10/01/16.
 */
public interface ControlInterface {
    void onAutoPlayChange(final boolean autoplay);
    void onTrackChange(@Nullable final AudioTrack track);
    void onProgressChange(final float progress);
    void onDataChange(final boolean hasData);
    void setTurntable(final Turntable turntable);
}
