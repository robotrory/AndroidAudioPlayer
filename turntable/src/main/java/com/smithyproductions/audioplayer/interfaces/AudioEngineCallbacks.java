package com.smithyproductions.audioplayer.interfaces;

import com.smithyproductions.audioplayer.AudioTrack;

/**
 * Created by rory on 07/01/16.
 */
public interface AudioEngineCallbacks {
    void onProgress(float progress);
    void onError();
    void onTrackChange(final AudioTrack track);
    void onAutoPlayStateChange(final boolean autoplay);
}
