package audioplayertest;

import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;

import com.smithyproductions.audioplayer.MediaRouterMultiplexer;
import com.smithyproductions.audioplayer.Turntable;
import com.smithyproductions.audioplayer.TurntableBuilder;
import com.smithyproductions.audioplayer.audioEngines.FadingAudioEngine;
import com.smithyproductions.audioplayer.audioEngines.PreloadingAudioEngine;
import com.smithyproductions.audioplayer.controls.AudioFocusControl;
import com.smithyproductions.audioplayer.controls.MediaSessionControl;
import com.smithyproductions.audioplayer.playerEngines.MediaPlayerEngine;

import audioplayertest.e8tracks.E8tracksActivity;
import audioplayertest.e8tracks.e8tracksNotificationControl;

/**
 * Created by rory on 21/01/16.
 */
public class AudioPlayerApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Turntable turntable = new TurntableBuilder(getApplicationContext())
                .setAudioEngine(new MediaRouterMultiplexer(getApplicationContext(), new FadingAudioEngine(MediaPlayerEngine.class)))
                .build();

        turntable.attachControl(AudioFocusControl.getInstance(this));

    }
}
