package com.smithyproductions.audioplayer;

import com.smithyproductions.audioplayer.audioEngines.BaseAudioEngine;
import com.smithyproductions.audioplayer.interfaces.AudioEngineCallbacks;
import com.smithyproductions.audioplayer.interfaces.ProgressListener;
import com.smithyproductions.audioplayer.playerEngines.BasePlayerEngine;
import com.smithyproductions.audioplayer.interfaces.MediaPlayerCallbacks;
import com.smithyproductions.audioplayer.trackProviders.TrackProvider;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by rory on 07/01/16.
 */
public class AudioPlayer implements AudioEngineCallbacks {

    private final TrackProvider trackProvider;
    private final Class<? extends BaseAudioEngine> audioEngineClass;
    private final Class<? extends BasePlayerEngine> mediaPlayerClass;

    private static AudioPlayer sAudioPlayer;

    private BaseAudioEngine baseAudioEngine;

    private Set<ProgressListener> progressListenerSet = new HashSet<>();


    private AudioPlayer(final TrackProvider trackProvider, final Class<? extends BaseAudioEngine> audioEngineClass, final Class<? extends BasePlayerEngine> mediaPlayerClass) {
        this.trackProvider = trackProvider;
        this.audioEngineClass = audioEngineClass;
        this.mediaPlayerClass = mediaPlayerClass;

        try {
            baseAudioEngine = audioEngineClass.newInstance();
            //todo think about passing a different callback inetrface here, maybe specially for UI?
            baseAudioEngine.init(mediaPlayerClass, this.trackProvider, this);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    static AudioPlayer initPlayer (final TrackProvider trackProvider, final Class<? extends BaseAudioEngine> audioEngineClass, final Class<? extends BasePlayerEngine> mediaPlayerClass) {
        if (sAudioPlayer != null) {
            throw new RuntimeException("You can only init audio player once");
        }

        sAudioPlayer = new AudioPlayer(trackProvider, audioEngineClass, mediaPlayerClass);

        return sAudioPlayer;
    }

    public static AudioPlayer getPlayer () {
        if (sAudioPlayer == null) {
            throw new RuntimeException("You need to set up audio player first");
        }

        return sAudioPlayer;
    }

    public void play() {
        baseAudioEngine.play();
    }

    public void pause() {
        baseAudioEngine.pause();
    }

    public void nextTrack() {
        baseAudioEngine.next();
    }

    public void previousTrack() {
        baseAudioEngine.previous();
    }

    public void addProgressListener(ProgressListener progressListener) {
        progressListenerSet.add(progressListener);
    }

    @Override
    public void onProgress(float progress) {
        for(ProgressListener progressListener : progressListenerSet) {
            progressListener.onProgress(progress);
        }
    }

    @Override
    public void onError() {
        throw new RuntimeException("Some error");
    }
}
