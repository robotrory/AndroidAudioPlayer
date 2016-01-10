package com.smithyproductions.audioplayer.interfaces;

import com.smithyproductions.audioplayer.AudioPlayer;
import com.smithyproductions.audioplayer.AudioTrack;

/**
 * Created by rory on 10/01/16.
 */
public interface ControlInterface {
    void onAutoPlayChange(final boolean autoplay);
    void onTrackChange(final AudioTrack track);
    void onProgressChange(final float progress);
    void onDataChange(final boolean hasData);
    void setAudioPlayer(final AudioPlayer audioPlayer);
}
