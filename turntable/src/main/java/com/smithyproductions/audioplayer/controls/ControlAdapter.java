package com.smithyproductions.audioplayer.controls;

import android.support.annotation.Nullable;

import com.smithyproductions.audioplayer.Turntable;
import com.smithyproductions.audioplayer.AudioTrack;
import com.smithyproductions.audioplayer.interfaces.ControlInterface;

/**
 * Created by rory on 10/01/16.
 */
public abstract class ControlAdapter implements ControlInterface {

    @Nullable protected Turntable turntable;
    protected boolean audioPlayerAttached;

    protected ControlAdapter() {
    }


    @Override
    public void setTurntable(Turntable turntable) {
        this.turntable = turntable;
        audioPlayerAttached = this.turntable != null;
    }

    @Override
    public void onAutoPlayChange(boolean autoplay) {

    }

    @Override
    public void onTrackChange(@Nullable AudioTrack track) {

    }

    @Override
    public void onProgressChange(float progress) {

    }

    @Override
    public void onDataChange(boolean hasData) {

    }
}
