package com.smithyproductions.audioplayer;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;

import com.smithyproductions.audioplayer.MediaRouter.MediaRouteManager;
import com.smithyproductions.audioplayer.audioEngines.BaseAudioEngine;
import com.smithyproductions.audioplayer.interfaces.AudioEngineCallbacks;
import com.smithyproductions.audioplayer.interfaces.ControlInterface;
import com.smithyproductions.audioplayer.interfaces.State;
import com.smithyproductions.audioplayer.playerEngines.BasePlayerEngine;
import com.smithyproductions.audioplayer.trackProviders.TrackProvider;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by rory on 07/01/16.
 */
public class Turntable {

    private final Class<? extends BaseAudioEngine> audioEngineClass;
    private final Class<? extends BasePlayerEngine> mediaPlayerClass;
    private static Turntable sTurntable;
    private final MediaRouteManager mMediaRouteManager;

    private BaseAudioEngine baseAudioEngine;

    private Set<ControlInterface> controlInterfaceSet = new HashSet<>();

    private @Nullable TrackProvider trackProvider;
    private Set<ControlInterface> queuedControlInterfaceSet = new HashSet<>();
    private AudioTrack currentTrack;
    private State currentState;
    private PersistentService service;
    private float lastProgress;
    private boolean chromecastEnabled;


    private Turntable(final Context context, final Class<? extends BaseAudioEngine> audioEngineClass, final Class<? extends BasePlayerEngine> mediaPlayerClass) {
        this.audioEngineClass = audioEngineClass;
        this.mediaPlayerClass = mediaPlayerClass;

        try {
            baseAudioEngine = audioEngineClass.newInstance();
            //todo think about passing a different callback inetrface here, maybe specially for UI?
            baseAudioEngine.init(mediaPlayerClass, context, audioEngineCallbacks);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        mMediaRouteManager = new MediaRouteManager(context);
        attachControl(mMediaRouteManager);

    }


    static Turntable initPlayer (final Context context, final Class<? extends BaseAudioEngine> audioEngineClass, final Class<? extends BasePlayerEngine> mediaPlayerClass) {
        if (sTurntable != null) {
            throw new RuntimeException("You can only init audio player once");
        }

        sTurntable = new Turntable(context, audioEngineClass, mediaPlayerClass);

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

    public void setChromecastEnabled(boolean chromecastEnabled) {
        this.chromecastEnabled = chromecastEnabled;
        mMediaRouteManager.setEnabled(this.chromecastEnabled);
    }

    public MediaRouteManager getMediaRouteManager() {
        return mMediaRouteManager;
    }
}
