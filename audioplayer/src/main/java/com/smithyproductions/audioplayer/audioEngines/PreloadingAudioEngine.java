package com.smithyproductions.audioplayer.audioEngines;

import android.util.Log;

import com.smithyproductions.audioplayer.AudioTrack;
import com.smithyproductions.audioplayer.interfaces.AudioEngineCallbacks;
import com.smithyproductions.audioplayer.playerEngines.BasePlayerEngine;
import com.smithyproductions.audioplayer.trackProviders.TrackProvider;

/**
 * Created by rory on 07/01/16.
 */
public class PreloadingAudioEngine extends BaseAudioEngine {

    protected TrackProvider trackProvider;
    BasePlayerEngine[] playerArray = new BasePlayerEngine[2];
    protected AudioEngineCallbacks parentCallbacks;

    @Override
    public void init(Class<? extends BasePlayerEngine> mediaPlayerClass, TrackProvider trackProvider, AudioEngineCallbacks callbacks) {
        this.parentCallbacks = callbacks;
        this.trackProvider = trackProvider;

        try {
            BasePlayerEngine engine1 = mediaPlayerClass.newInstance();
            engine1.setCallbackHandler(this);

            BasePlayerEngine engine2 = mediaPlayerClass.newInstance();

            playerArray[0] = engine1;
            playerArray[1] = engine2;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        //here we try to load any tracks we can get our hands on, regardless of whether it'll be played or not
        loadCurrentTracks();
    }

    @Override
    public void play() {
        if(playerArray[0].getTrack() != null) {
            if(playerArray[0].isFinished()) {
                movePlayersToNextTrack();
                playFromStart(playerArray[0]);
            } else {
                playerArray[0].play();
            }

        }else {
            loadCurrentTracks();
        }

        //player will play immediately if it can, otehrwise it'll play when
        //it's ready
        playerArray[0].play();
    }


    protected void loadCurrentTracks() {
        //todo what happens when this is called repeatedly and lots of callbacks are issued?
        trackProvider.requestNthTrack(trackProvider.getCurrentTrackIndex(), new TrackProvider.TrackCallback() {
            @Override
            public void onTrackRetrieved(AudioTrack track) {
                loadInTrack(track, playerArray[0]);
            }

            @Override
            public void onError(String errorMsg) {
                Log.d("PreloadingAudioEngine", "can't get current track: '" + errorMsg + "'");
            }
        });

        trackProvider.requestNthTrack(trackProvider.getCurrentTrackIndex() + 1, new TrackProvider.TrackCallback() {
            @Override
            public void onTrackRetrieved(AudioTrack track) {
                loadInTrack(track, playerArray[1]);
            }

            @Override
            public void onError(String errorMsg) {
                Log.d("PreloadingAudioEngine", "can't get next track: '" + errorMsg + "'");
            }
        });


    }

    private void loadInTrack(AudioTrack track, final BasePlayerEngine engine) {
        if(track != null) {
            AudioTrack loadedTrack = engine.getTrack();
            if(track.equals(loadedTrack)) {
                Log.d("PreloadingAudioEngine", "track already loaded");
            } else {
                engine.unloadCurrent();
                engine.loadTrack(track);
            }

        } else {
            throw new RuntimeException("No track here!");
        }
    }

    protected void swapEngines() {
        BasePlayerEngine tmp = playerArray[0];
        playerArray[0] = playerArray[1];
        playerArray[1] = tmp;

        playerArray[0].setCallbackHandler(this);
        playerArray[1].setCallbackHandler(null);

        //we don't want the secondary player to be playing
        playerArray[1].pause();
    }

    private void playFromStart(BasePlayerEngine engine) {
        //we only want to reset if it's in the background
        if(!engine.isPreparing()) {
            engine.seekTo(0);
        }
        engine.play();
    }

    @Override
    public void pause() {
        playerArray[0].pause();
    }

    @Override
    public void next() {
        movePlayersToNextTrack();
        playFromStart(playerArray[0]);
    }

    protected void movePlayersToNextTrack() {
        swapEngines();
        trackProvider.incrementTrackIndex();
        loadCurrentTracks();
    }

    private void movePlayersToPreviousTrack() {
        swapEngines();
        trackProvider.decrementTrackIndex();
        loadCurrentTracks();
    }

    @Override
    public void previous() {
        movePlayersToPreviousTrack();
        playerArray[0].play();
    }

    @Override
    public void onTrackFinished() {
        //this only happens when we're playing, so we should play the background engine
        movePlayersToNextTrack();
        playFromStart(playerArray[0]);
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
