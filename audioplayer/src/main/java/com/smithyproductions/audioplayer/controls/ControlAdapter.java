package com.smithyproductions.audioplayer.controls;

import android.support.annotation.Nullable;

import com.smithyproductions.audioplayer.AudioPlayer;
import com.smithyproductions.audioplayer.AudioTrack;
import com.smithyproductions.audioplayer.interfaces.ControlInterface;

/**
 * Created by rory on 10/01/16.
 */
public class ControlAdapter implements ControlInterface {

    @Nullable protected AudioPlayer audioPlayer;
    protected boolean audioPlayerAttached;

    public ControlAdapter () {
    }

    @Override
    public void setAudioPlayer(AudioPlayer audioPlayer) {
        this.audioPlayer = audioPlayer;
        audioPlayerAttached = this.audioPlayer != null;
    }

    @Override
    public void onAutoPlayChange(boolean autoplay) {

    }

    @Override
    public void onTrackChange(AudioTrack track) {

    }

    @Override
    public void onProgressChange(float progress) {

    }

    @Override
    public void onDataChange(boolean hasData) {

    }
}
