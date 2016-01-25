package com.smithyproductions.audioplayertest.e8tracks;

import android.app.PendingIntent;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.smithyproductions.audioplayer.controls.NotificationControl;

import java.util.ArrayList;

/**
 * Created by rory on 22/01/16.
 */
public class e8tracksNotificationControl extends NotificationControl {

    private static final String ACTION_NEXT_MIX = "next_mix";

    public e8tracksNotificationControl(Context context, PendingIntent openIntent) {
        super(context, openIntent);

        registerFilter(ACTION_NEXT_MIX);

    }

    @Override
    protected ArrayList<NotificationCompat.Action> getNotificationActions() {
        final ArrayList<android.support.v7.app.NotificationCompat.Action> actions = new ArrayList<>();

        actions.add(new android.support.v7.app.NotificationCompat.Action(android.R.drawable.ic_media_previous, "previous", createBroadcastIntent(turntable.getService(), NotificationControl.ACTION_PREVIOUS_TRACK, NotificationControl.REQUEST_CODE)));

        if (audioPlayerAttached && turntable.isAutoPlay()) {
            actions.add(new android.support.v7.app.NotificationCompat.Action(android.R.drawable.ic_media_pause, "pause", createBroadcastIntent(turntable.getService(), NotificationControl.ACTION_PAUSE, NotificationControl.REQUEST_CODE)));
        } else {
            actions.add(new android.support.v7.app.NotificationCompat.Action(android.R.drawable.ic_media_play, "play", createBroadcastIntent(turntable.getService(), NotificationControl.ACTION_PLAY, NotificationControl.REQUEST_CODE)));
        }
        if(audioPlayerAttached && turntable.getTrackProvider() != null && turntable.getTrackProvider() instanceof MixSetTrackProvider && !((MixSetTrackProvider) turntable.getTrackProvider()).canSkip()) {
            actions.add(new android.support.v7.app.NotificationCompat.Action(android.R.drawable.ic_media_ff, "next mix", createBroadcastIntent(turntable.getService(), ACTION_NEXT_MIX, NotificationControl.REQUEST_CODE)));
        } else {
            actions.add(new android.support.v7.app.NotificationCompat.Action(android.R.drawable.ic_media_next, "next", createBroadcastIntent(turntable.getService(), NotificationControl.ACTION_NEXT_TRACK, NotificationControl.REQUEST_CODE)));
        }

        return actions;
    }

    @Override
    protected void parseBroadcast(String action) {
        switch (action) {
            case ACTION_NEXT_MIX:
                Log.d("NotificationControl", "received next mix request");
                if(turntable.getTrackProvider() instanceof MixSetTrackProvider) {
                    ((MixSetTrackProvider) turntable.getTrackProvider()).goToNextMix();
                } else {
                    Log.d("NotificationControl", "can't go to next mix");
                }
                break;
            default:
                super.parseBroadcast(action);
        }

    }
}
