package com.smithyproductions.audioplayer.trackProviders;

import android.util.Log;

import com.smithyproductions.audioplayer.AudioTrack;

import java.util.List;

/**
 * Created by rory on 07/01/16.
 */
public class PlaylistTrackProvider extends TrackProvider {
    private int currentTrackIndex;

    private final List<AudioTrack> trackList;

    public PlaylistTrackProvider (List<AudioTrack> trackList) {
        this.trackList = trackList;
    }

    public AudioTrack getCurrentTrack() {
        return getNthTrack(0);
    }

    public AudioTrack getNthTrack(final int n) {
        return trackList.get(currentTrackIndex + n);
    }

    @Override
    public void requestNextTrack(NextTrackCallback callback) {
        currentTrackIndex++;
        if(currentTrackIndex < trackList.size()) {
            callback.onNextTrack(getCurrentTrack());
        } else {
            Log.d("PlaylistTrackProvider", "no more tracks to play, looping back to first");
            currentTrackIndex = 0;
            callback.onNextTrack(getCurrentTrack());
        }
    }

    @Override
    public void requestPreviousTrack(PreviousTrackCallback callback) {
        if(currentTrackIndex > 0) {
            currentTrackIndex--;
            callback.onPreviousTrack(getCurrentTrack());
        } else {
            callback.onError("Can't go to previous track, we're at the first!");
        }
    }
}
