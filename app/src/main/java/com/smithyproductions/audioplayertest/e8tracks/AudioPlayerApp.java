package com.smithyproductions.audioplayertest.e8tracks;

import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;

import com.smithyproductions.audioplayer.AudioPlayer;
import com.smithyproductions.audioplayer.AudioPlayerBuilder;
import com.smithyproductions.audioplayer.audioEngines.FadingAudioEngine;
import com.smithyproductions.audioplayer.controls.MediaSessionControl;
import com.smithyproductions.audioplayer.controls.NotificationControl;
import com.smithyproductions.audioplayer.playerEngines.MediaPlayerEngine;
import com.smithyproductions.audioplayertest.E8tracksActivity;

/**
 * Created by rory on 21/01/16.
 */
public class AudioPlayerApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        AudioPlayer audioPlayer = new AudioPlayerBuilder(getApplicationContext())
                .setPlayerEngine(MediaPlayerEngine.class)
                .setAudioEngine(FadingAudioEngine.class)
                .build();

        audioPlayer.attachControl(new MediaSessionControl());
        audioPlayer.attachControl(new NotificationControl(this, PendingIntent.getActivity(this, 0, new Intent(this, E8tracksActivity.class), 0)));
    }
}
