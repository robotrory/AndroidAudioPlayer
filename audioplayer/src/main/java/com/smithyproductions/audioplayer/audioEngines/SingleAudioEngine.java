package com.smithyproductions.audioplayer.audioEngines;

import android.util.Log;

import com.smithyproductions.audioplayer.AudioTrack;
import com.smithyproductions.audioplayer.interfaces.AudioEngineCallbacks;
import com.smithyproductions.audioplayer.trackProviders.TrackProvider;
import com.smithyproductions.audioplayer.playerEngines.BasePlayerEngine;

/**
 * Created by rory on 07/01/16.
 */
public class SingleAudioEngine extends BaseAudioEngine {

    private TrackProvider trackProvider;
    BasePlayerEngine playerImplementation;
    private AudioEngineCallbacks parentCallbacks;

    @Override
    public void init(Class<? extends BasePlayerEngine> mediaPlayerClass, TrackProvider trackProvider, AudioEngineCallbacks callbacks) {
        this.parentCallbacks = callbacks;
        this.trackProvider = trackProvider;
        try {
            playerImplementation = mediaPlayerClass.newInstance();
            playerImplementation.setCallbackHandler(this);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        //here we try to load any track we can get our hands on, regardless of whether it'll be played or not
        loadCurrentTrack();
    }

    @Override
    public void play() {
        if(playerImplementation.getTrack() != null) {
            if(playerImplementation.isFinished()) {
                //if we've finished the current track, but want to start, we should try and load the next one
                loadNextTrack();
            } else {
                //if we've loaded the track, we can start it
                playerImplementation.play();
            }
        }else {
            loadCurrentTrack();
        }
    }

    private void loadCurrentTrack() {
        //if we've finished the current track, spec dictates that we move onto the next track (if available)
        //todo what happens when this is called repeatedly and lots of callbacks are issued?
        trackProvider.requestNthTrack(trackProvider.getCurrentTrackIndex(), new TrackProvider.TrackCallback() {
            @Override
            public void onTrackRetrieved(AudioTrack track) {
                loadInTrack(track);
            }

            @Override
            public void onError(String errorMsg) {
                Log.d("SingleAudioEngine", "can't get current track: '" + errorMsg + "'");
            }
        });
    }

    private void loadNextTrack() {
        //if we've finished the current track, spec dictates that we move onto the next track (if available)
        //todo what happens when this is called repeatedly and lots of callbacks are issued?
        trackProvider.requestNthTrack(trackProvider.getCurrentTrackIndex() + 1, new TrackProvider.TrackCallback() {
            @Override
            public void onTrackRetrieved(AudioTrack track) {
                trackProvider.incrementTrackIndex();
                loadInTrack(track);
            }

            @Override
            public void onError(String errorMsg) {
                Log.d("SingleAudioEngine", "can't get next track: '" + errorMsg + "'");
            }
        });
    }

    private void loadPreviousTrack() {
        //if we've finished the current track, spec dictates that we move onto the next track (if available)
        //todo what happens when this is called repeatedly and lots of parentCallbacks are issued?
        trackProvider.requestNthTrack(trackProvider.getCurrentTrackIndex() - 1, new TrackProvider.TrackCallback() {
            @Override
            public void onTrackRetrieved(AudioTrack track) {
                trackProvider.decrementTrackIndex();
                loadInTrack(track);
            }

            @Override
            public void onError(String errorMsg) {
                Log.d("SingleAudioEngine", "can't get previous track: '" + errorMsg + "'");
            }
        });
    }

    private void loadInTrack(AudioTrack track) {
        if(track != null) {
            if(playerImplementation.getTrack() != null) {
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
    }

    @Override
    public void next() {
        loadNextTrack();
    }

    @Override
    public void previous() {
        loadPreviousTrack();
    }

    @Override
    public void onTrackFinished() {
        loadNextTrack();
    }

    @Override
    public void onProgress(float progress) {
        parentCallbacks.onProgress(progress);
    }

    @Override
    public void onError() {
        parentCallbacks.onError();
    }
}
