package com.smithyproductions.audioplayer;

import android.content.Context;

import com.smithyproductions.audioplayer.audioEngines.BaseAudioEngine;
import com.smithyproductions.audioplayer.audioEngines.SingleAudioEngine;
import com.smithyproductions.audioplayer.interfaces.AudioEngineInterface;
import com.smithyproductions.audioplayer.playerEngines.BasePlayerEngine;
import com.smithyproductions.audioplayer.playerEngines.MockPlayerEngine;

/**
 * Created by rory on 09/01/16.
 */
public class TurntableBuilder {

    private final Context context;
    private AudioEngineInterface audioEngine;

    public TurntableBuilder(final Context context) {
        this.context = context;
    }

    public Turntable build() {
        if (audioEngine == null) {
            audioEngine = new SingleAudioEngine(MockPlayerEngine.class);
        }

        return Turntable.initPlayer(context, audioEngine);
    }

    public TurntableBuilder setAudioEngine(AudioEngineInterface audioEngine) {
        this.audioEngine = audioEngine;
        return this;
    }
}
