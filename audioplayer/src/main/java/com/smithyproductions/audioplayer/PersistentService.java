package com.smithyproductions.audioplayer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by rory on 10/01/16.
 */
public class PersistentService extends Service {

    public final static String ACTION_INIT_PLAYER = "init_player";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if(AudioPlayer.getPlayer() != null) {
            AudioPlayer.getPlayer().bindService(this);
        }
    }
}
