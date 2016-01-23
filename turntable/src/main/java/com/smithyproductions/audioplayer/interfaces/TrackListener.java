package com.smithyproductions.audioplayer.interfaces;

import android.support.annotation.Nullable;

import com.smithyproductions.audioplayer.AudioTrack;

/**
 * Created by rory on 09/01/16.
 */
public interface TrackListener {
    void onTrackChange(@Nullable final AudioTrack track);
}
