package com.smithyproductions.audioplayer.trackProviders;

import com.smithyproductions.audioplayer.AudioTrack;

import java.util.List;

/**
 * Created by rory on 07/01/16.
 */
public abstract class TrackProvider {

    public abstract AudioTrack getCurrentTrack();

    public abstract AudioTrack getNthTrack(final int n);

    public abstract void requestNextTrack(NextTrackCallback callback);

    public abstract void requestPreviousTrack(PreviousTrackCallback callback);

    public interface NextTrackCallback {
        void onNextTrack(AudioTrack track);
        void onError(String errorMsg);
    }

    public interface PreviousTrackCallback {
        void onPreviousTrack(AudioTrack track);
        void onError(String errorMsg);
    }
}
