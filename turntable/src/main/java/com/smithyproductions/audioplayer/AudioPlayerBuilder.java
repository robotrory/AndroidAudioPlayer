package com.smithyproductions.audioplayer;

import android.content.Context;

import com.smithyproductions.audioplayer.audioEngines.BaseAudioEngine;
import com.smithyproductions.audioplayer.audioEngines.SingleAudioEngine;
import com.smithyproductions.audioplayer.playerEngines.BasePlayerEngine;
import com.smithyproductions.audioplayer.playerEngines.MockPlayerEngine;
import com.smithyproductions.audioplayer.trackProviders.TrackProvider;

/**
 * Created by rory on 09/01/16.
 */
public class AudioPlayerBuilder {

    private final Context context;
    private Class<? extends BaseAudioEngine> audioEngineClass = SingleAudioEngine.class;
    private Class<? extends BasePlayerEngine> mediaPlayerClass = MockPlayerEngine.class;

    public AudioPlayerBuilder (final Context context) {
        this.context = context;
    }

    public AudioPlayer build() {
        return AudioPlayer.initPlayer(context, audioEngineClass, mediaPlayerClass);
    }

    public AudioPlayerBuilder setPlayerEngine(Class<? extends BasePlayerEngine> playerEngine) {
        this.mediaPlayerClass = playerEngine;
        return this;
    }

    public AudioPlayerBuilder setAudioEngine(Class<? extends BaseAudioEngine> audioEngine) {
        this.audioEngineClass = audioEngine;
        return this;
    }
}
