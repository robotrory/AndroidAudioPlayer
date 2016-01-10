package com.smithyproductions.audioplayer;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.smithyproductions.audioplayer.audioEngines.BaseAudioEngine;
import com.smithyproductions.audioplayer.interfaces.AudioEngineCallbacks;
import com.smithyproductions.audioplayer.interfaces.ControlInterface;
import com.smithyproductions.audioplayer.interfaces.PlaybackListener;
import com.smithyproductions.audioplayer.interfaces.ProgressListener;
import com.smithyproductions.audioplayer.interfaces.State;
import com.smithyproductions.audioplayer.interfaces.TrackListener;
import com.smithyproductions.audioplayer.playerEngines.BasePlayerEngine;
import com.smithyproductions.audioplayer.trackProviders.TrackProvider;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by rory on 07/01/16.
 */
public class AudioPlayer implements AudioEngineCallbacks, TrackProvider.TrackProviderListener {

    private final TrackProvider trackProvider;
    private final Class<? extends BaseAudioEngine> audioEngineClass;
    private final Class<? extends BasePlayerEngine> mediaPlayerClass;

    private static AudioPlayer sAudioPlayer;

    private BaseAudioEngine baseAudioEngine;

    private Set<ControlInterface> controlInterfaceSet = new HashSet<>();
    private AudioTrack currentTrack;
    private State currentState;
    private PersistentService service;
    private float lastProgress;


    private AudioPlayer(final TrackProvider trackProvider, final Context context, final Class<? extends BaseAudioEngine> audioEngineClass, final Class<? extends BasePlayerEngine> mediaPlayerClass) {
        this.trackProvider = trackProvider;
        this.audioEngineClass = audioEngineClass;
        this.mediaPlayerClass = mediaPlayerClass;

        try {
            baseAudioEngine = audioEngineClass.newInstance();
            //todo think about passing a different callback inetrface here, maybe specially for UI?
            baseAudioEngine.init(mediaPlayerClass, context, this.trackProvider, this);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        this.trackProvider.attachListener(this);
    }


    static AudioPlayer initPlayer (final Context context, final TrackProvider trackProvider, final Class<? extends BaseAudioEngine> audioEngineClass, final Class<? extends BasePlayerEngine> mediaPlayerClass) {
        if (sAudioPlayer != null) {
            throw new RuntimeException("You can only init audio player once");
        }

        sAudioPlayer = new AudioPlayer(trackProvider, context, audioEngineClass, mediaPlayerClass);

        final Intent service = new Intent(context, PersistentService.class);
        service.setAction(PersistentService.ACTION_INIT_PLAYER);
        context.startService(service);

        return sAudioPlayer;
    }

    public static AudioPlayer getPlayer () {
        return sAudioPlayer;
    }

    public void play() {
        if(hasData()) {
            baseAudioEngine.play();
        } else {
            Log.e("AudioPlayer", "disallowing play as we have no data");
        }
    }

    public void pause() {
        if(hasData()) {
            baseAudioEngine.pause();
        } else {
            Log.e("AudioPlayer", "disallowing pause as we have no data");
        }
    }

    public void nextTrack() {
        if(hasData()) {
            baseAudioEngine.next();
        } else {
            Log.e("AudioPlayer", "disallowing next track as we have no data");
        }
    }

    public void previousTrack() {
        if(hasData()) {
            baseAudioEngine.previous();
        } else {
            Log.e("AudioPlayer", "disallowing previous track as we have no data");
        }
    }

    public void attachControl(final ControlInterface controlInterface) {
        controlInterfaceSet.add(controlInterface);

        controlInterface.onProgressChange(lastProgress);
        controlInterface.onDataChange(hasData());
        controlInterface.onAutoPlayChange(isAutoPlay());
        controlInterface.onTrackChange(getTrack());

        controlInterface.setAudioPlayer(this);
    }

    public void unattachControl(final ControlInterface controlInterface) {
        controlInterface.setAudioPlayer(null);
        controlInterfaceSet.remove(controlInterface);
    }

    @Override
    public void onProgress(float progress) {
        lastProgress = progress;
        for(ControlInterface controlInterface : controlInterfaceSet) {
            controlInterface.onProgressChange(progress);
        }
    }

    @Override
    public void onTrackChange(AudioTrack track) {
        currentTrack = track;
        for(ControlInterface controlInterface : controlInterfaceSet) {
            controlInterface.onTrackChange(track);
        }
    }

    @Override
    public void onAutoPlayStateChange(final boolean autoplay) {
        for(ControlInterface controlInterface : controlInterfaceSet) {
            controlInterface.onAutoPlayChange(autoplay);
        }
    }

    @Override
    public void onDataInvalidated() {
        final boolean hasData = trackProvider.getTrackCount() > 0;
        for(ControlInterface controlInterface : controlInterfaceSet) {
            controlInterface.onDataChange(hasData);
        }
    }

    @Override
    public void onError() {
        throw new RuntimeException("Some error");
    }

    public AudioTrack getTrack() {
        return currentTrack;
    }

    public boolean isAutoPlay() {
        return baseAudioEngine.willAutoPlay();
    }

    public void bindService(PersistentService service) {
        this.service = service;
    }

    public PersistentService getService() {
        return service;
    }

    public boolean hasData() {
        return trackProvider.getTrackCount() > 0;
    }

    public void stop () {
        trackProvider.reset();
        baseAudioEngine.reset();
    }
}
