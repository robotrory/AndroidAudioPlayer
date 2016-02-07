package com.smithyproductions.audioplayer;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import com.smithyproductions.audioplayer.MediaRouter.MediaRouteManager;
import com.smithyproductions.audioplayer.audioEngines.BaseAudioEngine;
import com.smithyproductions.audioplayer.interfaces.AudioEngineCallbacks;
import com.smithyproductions.audioplayer.interfaces.AudioEngineInterface;
import com.smithyproductions.audioplayer.interfaces.ControlInterface;
import com.smithyproductions.audioplayer.interfaces.ControlType;
import com.smithyproductions.audioplayer.interfaces.State;
import com.smithyproductions.audioplayer.playerEngines.BasePlayerEngine;
import com.smithyproductions.audioplayer.trackProviders.TrackProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by rory on 07/01/16.
 */
public class Turntable {

    private static Turntable sTurntable;

    private AudioEngineInterface baseAudioEngine;

    private Set<ControlInterface> controlInterfaceSet = new HashSet<>();

    private @Nullable TrackProvider trackProvider;
    private Set<ControlInterface> queuedControlInterfaceSet = new HashSet<>();
    private AudioTrack currentTrack;
    private State currentState;
    private PersistentService service;
    private float lastProgress;

    List<ControlType> uniqueControlTypes = new ArrayList<ControlType>() {{
        add(ControlType.MEDIASESSION);
        add(ControlType.NOTIFICATION);
    }};


    private Turntable(final Context context, final AudioEngineInterface audioEngine) {
        this.baseAudioEngine = audioEngine;

        baseAudioEngine.init(context, audioEngineCallbacks);

    }


    static Turntable initPlayer (final Context context, final AudioEngineInterface audioEngine) {
        if (sTurntable != null) {
            throw new RuntimeException("You can only init audio player once");
        }

        sTurntable = new Turntable(context, audioEngine);

        final Intent service = new Intent(context, PersistentService.class);
        service.setAction(PersistentService.ACTION_INIT_PLAYER);
        context.startService(service);

        return sTurntable;
    }

    public static Turntable getPlayer () {
        return sTurntable;
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
        //control interface expects us to be fully functional
        //we're not fully functional until our service has been attached
        if (service != null) {
            addControlToSet(controlInterface);
        } else {
            queuedControlInterfaceSet.add(controlInterface);
        }
    }

    private void addControlToSet(ControlInterface controlInterface) {


        if(!controlInterfaceSet.contains(controlInterface)) {

            //if we have an interface of that type already then remove the existing one
            //this is to safeguard against multiple mediasession controllers, for instance
            if (uniqueControlTypes.contains(controlInterface.getControlType())) {
                Iterator<ControlInterface> iterator = controlInterfaceSet.iterator();
                while(iterator.hasNext()) {
                    ControlInterface existingInterface = iterator.next();
                    if (existingInterface.getControlType() == controlInterface.getControlType()) {
                        iterator.remove();
                    }
                }
            }

            controlInterface.onProgressChange(lastProgress);
            controlInterface.onDataChange(hasData());
            controlInterface.onAutoPlayChange(isAutoPlay());
            controlInterface.onTrackChange(getTrack());

            controlInterface.setTurntable(this);
            controlInterfaceSet.add(controlInterface);
        }
    }

    public void detachControl(final ControlInterface controlInterface) {
        controlInterface.setTurntable(null);
        controlInterfaceSet.remove(controlInterface);
    }

    final AudioEngineCallbacks audioEngineCallbacks = new AudioEngineCallbacks() {
        @Override
        public void onProgress(float progress) {
            lastProgress = progress;
            for (ControlInterface controlInterface : controlInterfaceSet) {
                controlInterface.onProgressChange(progress);
            }
        }

        @Override
        public void onTrackChange(AudioTrack track) {
            currentTrack = track;
            for (ControlInterface controlInterface : controlInterfaceSet) {
                controlInterface.onTrackChange(track);
            }
        }

        @Override
        public void onAutoPlayStateChange(final boolean autoplay) {
            for (ControlInterface controlInterface : controlInterfaceSet) {
                controlInterface.onAutoPlayChange(autoplay);
            }
        }

        @Override
        public void onError() {
            throw new RuntimeException("Some error");
        }
    };



    final TrackProvider.TrackProviderListener trackProviderListener = new TrackProvider.TrackProviderListener() {
        @Override
        public void onTracksInvalidated() {
            for (ControlInterface controlInterface : controlInterfaceSet) {
                controlInterface.onDataChange(hasData());
            }
        }
    };

    public AudioTrack getTrack() {
        return currentTrack;
    }

    public boolean isAutoPlay() {
        return baseAudioEngine.willAutoPlay();
    }

    void bindService(PersistentService service) {
        this.service = service;

        if(this.service != null) {
            //now add any queued controls
            for (ControlInterface controlInterface : queuedControlInterfaceSet) {
                addControlToSet(controlInterface);
            }

            queuedControlInterfaceSet.clear();
        } else {
            //no service for whatever reason, so controls are non-functional
            for (ControlInterface controlInterface : controlInterfaceSet) {
                controlInterface.setTurntable(null);
                controlInterfaceSet.add(controlInterface);
            }
            controlInterfaceSet.clear();
        }
    }

    public PersistentService getService() {
        return service;
    }

    public boolean hasData() {
        if (trackProvider != null) {
            return trackProvider.getTrackCount() > 0;
        } else {
            return false;
        }
    }

    public void stop () {
        if (trackProvider != null) {
            trackProvider.reset();
        }
        baseAudioEngine.reset();
    }

    public void setTrackProvider(@Nullable TrackProvider trackProvider) {
        if(this.trackProvider != null) {
            this.trackProvider.dettachListener(trackProviderListener);
        }
        this.trackProvider = trackProvider;
        baseAudioEngine.setTrackProvider(this.trackProvider);

        if(this.trackProvider != null) {
            this.trackProvider.attachListener(trackProviderListener);
        }
    }

    @Nullable
    public TrackProvider getTrackProvider() {
        return trackProvider;
    }

    public void setVolume(float volume) {
        this.baseAudioEngine.setVolume(volume);
    }

}
