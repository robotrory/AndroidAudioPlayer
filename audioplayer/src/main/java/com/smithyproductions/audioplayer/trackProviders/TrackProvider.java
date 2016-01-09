package com.smithyproductions.audioplayer.trackProviders;

import com.smithyproductions.audioplayer.AudioTrack;

import java.util.List;

/**
 * Created by rory on 07/01/16.
 */
public abstract class TrackProvider {

    public abstract int getCurrentTrackIndex();

    public abstract void decrementTrackIndex();
    public abstract void incrementTrackIndex();

    public abstract void requestNthTrack(final int n, TrackCallback callback);


    public interface TrackCallback {
        void onTrackRetrieved(AudioTrack track);
        void onError(String errorMsg);
    }
}
