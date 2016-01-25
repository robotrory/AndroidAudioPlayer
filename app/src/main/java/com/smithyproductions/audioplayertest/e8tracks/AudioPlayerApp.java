package com.smithyproductions.audioplayertest.e8tracks;

import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;

import com.smithyproductions.audioplayer.Turntable;
import com.smithyproductions.audioplayer.TurntableBuilder;
import com.smithyproductions.audioplayer.audioEngines.FadingAudioEngine;
import com.smithyproductions.audioplayer.controls.AudioFocusControl;
import com.smithyproductions.audioplayer.controls.MediaSessionControl;
import com.smithyproductions.audioplayer.playerEngines.MediaPlayerEngine;
import com.smithyproductions.audioplayertest.E8tracksActivity;

/**
 * Created by rory on 21/01/16.
 */
public class AudioPlayerApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Turntable turntable = new TurntableBuilder(getApplicationContext())
                .setPlayerEngine(MediaPlayerEngine.class)
                .setAudioEngine(FadingAudioEngine.class)
                .build();

        turntable.attachControl(new AudioFocusControl(this));
        turntable.attachControl(new MediaSessionControl());
        turntable.attachControl(new e8tracksNotificationControl(this, PendingIntent.getActivity(this, 0, new Intent(this, E8tracksActivity.class), 0)));

        turntable.setChromecastEnabled(true);
    }
}
