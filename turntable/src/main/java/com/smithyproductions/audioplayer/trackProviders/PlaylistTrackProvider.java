package com.smithyproductions.audioplayer.trackProviders;

import android.util.Log;

import com.smithyproductions.audioplayer.AudioTrack;

import java.util.List;

/**
 * Created by rory on 07/01/16.
 */
public class PlaylistTrackProvider extends TrackProvider {
    protected int currentTrackIndex;

    protected final List<AudioTrack> trackList;

    public PlaylistTrackProvider (List<AudioTrack> trackList) {
        this.trackList = trackList;
    }

    @Override
    public int getCurrentTrackIndex() {
        return currentTrackIndex;
    }

    @Override
    public void decrementTrackIndex() {
        currentTrackIndex--;
        ensureValidTrackIndex();
    }

    @Override
    public void incrementTrackIndex() {
        currentTrackIndex++;
        ensureValidTrackIndex();
    }

    protected void ensureValidTrackIndex() {
        if(currentTrackIndex >= trackList.size()) {
            currentTrackIndex = 0;
        } else if(currentTrackIndex <= 0) {
            currentTrackIndex = 0;
        }
    }

    @Override
    public void requestNthTrack(int n, TrackCallback callback) {
        if(n >= trackList.size()) {
            Log.d("PlaylistTrackProvider", "no more tracks to play, looping back to first");
            callback.onTrackRetrieved(trackList.get(0));
        } else if (n >= 0) {
            callback.onTrackRetrieved(trackList.get(n));
        } else {
            callback.onError("Can't go to previous track, we're at the first!");
        }
    }

    @Override
    public void cancelAllTrackRequests() {
        //noop, we don't ever store the requests
    }

    @Override
    public int getTrackCount() {
        return trackList.size();
    }

    @Override
    public List<AudioTrack> getTrackList() {
        return trackList;
    }

    @Override
    public void reset() {
        currentTrackIndex = 0;

        for(TrackProviderListener trackProviderListener : trackProviderListenerSet) {
            trackProviderListener.onTracksInvalidated();
        }
    }
}
