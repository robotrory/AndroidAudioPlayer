package com.smithyproductions.audioplayer;

import com.smithyproductions.audioplayer.audioEngines.BaseAudioEngine;
import com.smithyproductions.audioplayer.audioEngines.SingleAudioEngine;
import com.smithyproductions.audioplayer.playerEngines.BasePlayerEngine;
import com.smithyproductions.audioplayer.playerEngines.MediaPlayerEngine;
import com.smithyproductions.audioplayer.playerEngines.MockPlayerEngine;
import com.smithyproductions.audioplayer.trackProviders.PlaylistTrackProvider;

/**
 * Created by rory on 09/01/16.
 */
public class AudioPlayerBuilder {

    private PlaylistTrackProvider trackProvider;
    private Class<? extends BaseAudioEngine> audioEngineClass = SingleAudioEngine.class;
    private Class<? extends BasePlayerEngine> mediaPlayerClass = MockPlayerEngine.class;

    public AudioPlayer build() {
        if(trackProvider == null) {
            throw new RuntimeException("You must call setTrackProvider() before build()");
        }
        return AudioPlayer.initPlayer(trackProvider, audioEngineClass, mediaPlayerClass);
    }

    public AudioPlayerBuilder setTrackProvider(PlaylistTrackProvider trackProvider) {
        this.trackProvider = trackProvider;
        return this;
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
