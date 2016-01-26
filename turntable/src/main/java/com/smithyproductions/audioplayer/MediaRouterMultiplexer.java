package com.smithyproductions.audioplayer;

import android.content.Context;
import android.support.annotation.NonNull;

import com.smithyproductions.audioplayer.MediaRouter.MediaRouteManager;
import com.smithyproductions.audioplayer.interfaces.AudioEngineCallbacks;
import com.smithyproductions.audioplayer.interfaces.AudioEngineInterface;
import com.smithyproductions.audioplayer.trackProviders.TrackProvider;

import java.sql.Ref;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by rory on 26/01/16.
 */
public class MediaRouterMultiplexer implements AudioEngineInterface, MediaRouteManager.MultiplexerControlInterface {

    public static final String LOCAL_KEY = "local";
    public static final String REMOTE_KEY = "remote";

    private Map<String, AudioEngineInterface> interfaceMap = new HashMap<>();
    private String currentEngineKey;
    private AudioEngineCallbacks parentCallbacks;
    private TrackProvider trackProvider;

    public MediaRouterMultiplexer(final Context context, @NonNull AudioEngineInterface localEngine) {
        addAudioEngine(LOCAL_KEY, localEngine, true);

        addAudioEngine(REMOTE_KEY, new MediaRouteManager(context, this));
    }

    @Override
    public void init(Context context, @NonNull AudioEngineCallbacks callbacks) {
        this.parentCallbacks = callbacks;

        interfaceMap.get(LOCAL_KEY).init(context, new AudioCallbackShim(LOCAL_KEY, callbacks));
        interfaceMap.get(REMOTE_KEY).init(context, new AudioCallbackShim(REMOTE_KEY, callbacks));
    }

    public void addAudioEngine(final String key, final AudioEngineInterface engineInterface, final boolean useNow) {
        addAudioEngine(key, engineInterface);

        if (useNow) {
            setCurrentAudioEngine(key);
        }
    }

    public void addAudioEngine(final String key, final AudioEngineInterface engineInterface) {
        interfaceMap.put(key, engineInterface);
    }

    public void setCurrentAudioEngine(final String key) {
        this.currentEngineKey = key;

    }

    public void removeAudioEngine(final String key) {
        interfaceMap.remove(key);
    }

    @Override
    public void play() {
        interfaceMap.get(currentEngineKey).play();
    }

    @Override
    public void pause() {
        interfaceMap.get(currentEngineKey).pause();
    }

    @Override
    public void next() {
        interfaceMap.get(currentEngineKey).next();
    }

    @Override
    public void previous() {
        interfaceMap.get(currentEngineKey).previous();
    }

    @Override
    public boolean willAutoPlay() {
        return interfaceMap.get(currentEngineKey).willAutoPlay();
    }

    @Override
    public void setVolume(float volume) {
        interfaceMap.get(currentEngineKey).setVolume(volume);
    }

    @Override
    public int getPlaybackPosition() {
        return interfaceMap.get(currentEngineKey).getPlaybackPosition();
    }

    @Override
    public void setPlaybackPosition(int position) {
        interfaceMap.get(currentEngineKey).setPlaybackPosition(position);
    }

    @Override
    public void reset() {

    }

    @Override
    public void setTrackProvider(TrackProvider trackProvider) {
        this.trackProvider = trackProvider;
        interfaceMap.get(currentEngineKey).setTrackProvider(trackProvider);
    }

    @Override
    public void requestMediaRouteFocus() {
        //move to chromecast

        if (LOCAL_KEY.equals(currentEngineKey)) {
            //what state are we in currently?
            final boolean autoplay = interfaceMap.get(LOCAL_KEY).willAutoPlay();
            final int position = interfaceMap.get(LOCAL_KEY).getPlaybackPosition();
            currentEngineKey = REMOTE_KEY;
            interfaceMap.get(LOCAL_KEY).setTrackProvider(null);
            interfaceMap.get(REMOTE_KEY).setTrackProvider(trackProvider);

            interfaceMap.get(LOCAL_KEY).reset();

            if (autoplay) {
                interfaceMap.get(REMOTE_KEY).play();
            } else {
                interfaceMap.get(REMOTE_KEY).pause();
            }

            interfaceMap.get(REMOTE_KEY).setPlaybackPosition(position);
        }
    }

    @Override
    public void notifyMediaRouteLost() {
        //move to local

        if (REMOTE_KEY.equals(currentEngineKey)) {
            //what state are we in currently?
            final boolean autoplay = interfaceMap.get(REMOTE_KEY).willAutoPlay();
            final int position = interfaceMap.get(REMOTE_KEY).getPlaybackPosition();
            currentEngineKey = LOCAL_KEY;
            interfaceMap.get(REMOTE_KEY).setTrackProvider(null);
            interfaceMap.get(LOCAL_KEY).setTrackProvider(trackProvider);

            interfaceMap.get(REMOTE_KEY).reset();

            if (autoplay) {
                interfaceMap.get(LOCAL_KEY).play();
            } else {
                interfaceMap.get(LOCAL_KEY).pause();
            }

            interfaceMap.get(LOCAL_KEY).setPlaybackPosition(position);
        }
    }

    private class AudioCallbackShim implements AudioEngineCallbacks {

        private final String engineKey;
        private final AudioEngineCallbacks parentCallbacks;

        public AudioCallbackShim (final String engineKey, final AudioEngineCallbacks parentCallbacks) {
            this.engineKey = engineKey;
            this.parentCallbacks = parentCallbacks;
        }

        @Override
        public void onProgress(float progress) {
            if (engineKey.equals(currentEngineKey)) {
                parentCallbacks.onProgress(progress);
            }
        }

        @Override
        public void onError() {
            if (engineKey.equals(currentEngineKey)) {
                parentCallbacks.onError();
            }
        }

        @Override
        public void onTrackChange(AudioTrack track) {
            if (engineKey.equals(currentEngineKey)) {
                parentCallbacks.onTrackChange(track);
            }
        }

        @Override
        public void onAutoPlayStateChange(boolean autoplay) {
            if (engineKey.equals(currentEngineKey)) {
                parentCallbacks.onAutoPlayStateChange(autoplay);
            }
        }
    }

}
