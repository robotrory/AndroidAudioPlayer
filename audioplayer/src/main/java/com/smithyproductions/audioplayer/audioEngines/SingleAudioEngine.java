package com.smithyproductions.audioplayer.audioEngines;

import android.content.Context;
import android.util.Log;

import com.smithyproductions.audioplayer.AudioTrack;
import com.smithyproductions.audioplayer.interfaces.AudioEngineCallbacks;
import com.smithyproductions.audioplayer.playerEngines.BasePlayerEngine;
import com.smithyproductions.audioplayer.trackProviders.TrackProvider;

/**
 * Created by rory on 07/01/16.
 */
public class SingleAudioEngine extends BaseAudioEngine {


    BasePlayerEngine playerImplementation;

    @Override
    public void init(Class<? extends BasePlayerEngine> mediaPlayerClass, final Context context, AudioEngineCallbacks callbacks) {
        this.parentCallbacks = callbacks;
        playerImplementation = createBasePlayerEngine(mediaPlayerClass, context);
        playerImplementation.setCallbackHandler(this);
    }

    @Override
    public void reset() {
        playerImplementation.pause();
        setAutoPlay(false);
        playerImplementation.unloadCurrent();
    }

    @Override
    public void play() {
        if (playerImplementation.getTrack() != null) {
            if (playerImplementation.isFinished()) {
                //if we've finished the current track, but want to start, we should try and load the next one
                loadTrack(1);
            } else {
                //if we've loaded the track, we can start it
                playerImplementation.play();
                setAutoPlay(true);
            }
        } else if (trackProvider != null && trackProvider.getTrackCount() > 0) {
            loadTrack(0);
        }
    }

    private void loadTrack(final int offset) {
        //if we've finished the current track, spec dictates that we move onto the next track (if available)
        parentCallbacks.onTrackChange(null);
        if (trackProvider != null) {
            trackProvider.cancelAllTrackRequests();
            trackProvider.requestNthTrack(trackProvider.getCurrentTrackIndex() + offset, new TrackProvider.TrackCallback() {
                @Override
                public void onTrackRetrieved(AudioTrack track) {
                    loadInTrack(track);
                    parentCallbacks.onTrackChange(track);

                    if (trackProvider != null) {
                        switch (offset) {
                            case 1:
                                trackProvider.incrementTrackIndex();
                                break;
                            case -1:
                                trackProvider.decrementTrackIndex();
                                break;
                        }
                    }
                }

                @Override
                public void onError(String errorMsg) {
                    Log.d("SingleAudioEngine", "can't get current track: '" + errorMsg + "'");
                }
            });
        }
    }

    private void loadInTrack(AudioTrack track) {
        if (track != null) {
            if (playerImplementation.getTrack() != null) {
                playerImplementation.unloadCurrent();
            }
            playerImplementation.loadTrack(track);
        } else {
            throw new RuntimeException("No track here!");
        }
    }

    @Override
    public void pause() {
        playerImplementation.pause();
        setAutoPlay(false);
    }

    @Override
    public void next() {
        playerImplementation.pause();
        loadTrack(1);
    }

    @Override
    public void previous() {
        playerImplementation.pause();
        loadTrack(-1);
    }

    @Override
    public void onTrackFinished() {
        loadTrack(1);
    }

    @Override
    public void onProgress(float progress) {
        parentCallbacks.onProgress(progress);
    }

    @Override
    public void onGeneralError() {
        parentCallbacks.onError();
    }

    @Override
    public void onTrackUnplayable() {
        next();
    }

    @Override
    public void onTracksInvalidated() {
        if (trackProvider != null && trackProvider.getTrackCount() > 0) {
            loadTrack(0);
        }
    }
}
