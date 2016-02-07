package com.smithyproductions.audioplayer.trackProviders;

import android.support.annotation.Nullable;

import com.smithyproductions.audioplayer.AudioTrack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by rory on 07/01/16.
 */
public abstract class TrackProvider {

    protected final Set<TrackProviderListener> trackProviderListenerSet = new HashSet<>();

    public void attachListener(TrackProviderListener trackProviderListener) {
        trackProviderListenerSet.add(trackProviderListener);
    }

    public void dettachListener(TrackProviderListener trackProviderListener) {
        trackProviderListenerSet.remove(trackProviderListener);
    }

    public abstract int getCurrentTrackIndex();

    public abstract void decrementTrackIndex();
    public abstract void incrementTrackIndex();

    public abstract void requestNthTrack(final int n, TrackCallback callback);

    public abstract void cancelAllTrackRequests();

    public abstract int getTrackCount();

    public abstract List<AudioTrack> getTrackList();

    public abstract void reset();

    public interface TrackCallback {
        void onTrackRetrieved(AudioTrack track);
        void onError(String errorMsg);
    }

    public interface TrackProviderListener {
        void onTracksInvalidated();
    }
}
