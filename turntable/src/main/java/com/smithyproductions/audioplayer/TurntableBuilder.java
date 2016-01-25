package com.smithyproductions.audioplayer;

import android.content.Context;

import com.smithyproductions.audioplayer.audioEngines.BaseAudioEngine;
import com.smithyproductions.audioplayer.audioEngines.SingleAudioEngine;
import com.smithyproductions.audioplayer.playerEngines.BasePlayerEngine;
import com.smithyproductions.audioplayer.playerEngines.MockPlayerEngine;

/**
 * Created by rory on 09/01/16.
 */
public class TurntableBuilder {

    private final Context context;
    private Class<? extends BaseAudioEngine> audioEngineClass = SingleAudioEngine.class;
    private Class<? extends BasePlayerEngine> mediaPlayerClass = MockPlayerEngine.class;

    public TurntableBuilder(final Context context) {
        this.context = context;
    }

    public Turntable build() {
        return Turntable.initPlayer(context, audioEngineClass, mediaPlayerClass);
    }

    public TurntableBuilder setPlayerEngine(Class<? extends BasePlayerEngine> playerEngine) {
        this.mediaPlayerClass = playerEngine;
        return this;
    }

    public TurntableBuilder setAudioEngine(Class<? extends BaseAudioEngine> audioEngine) {
        this.audioEngineClass = audioEngine;
        return this;
    }
}
