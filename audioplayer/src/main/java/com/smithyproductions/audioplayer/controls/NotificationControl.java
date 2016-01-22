package com.smithyproductions.audioplayer.controls;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.smithyproductions.audioplayer.AudioTrack;
import com.smithyproductions.audioplayer.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rory on 10/01/16.
 */
public class NotificationControl extends ControlAdapter {

    private static final int NOTIFICATION_ID = 2134;
    protected static final String ACTION_NEXT_TRACK = "next_track";
    protected static final String ACTION_PREVIOUS_TRACK = "previous_track";
    protected static final String ACTION_PAUSE = "pause";
    protected static final String ACTION_PLAY = "play";
    protected static final String ACTION_DELETE = "delete";
    protected static final int REQUEST_CODE = 3110;
    private final PendingIntent openIntent;
    private NotificationManager mNotificationManager;
    private boolean mStarted;
    private boolean mDismissable;
    private List<String> filterActions = new ArrayList<>();

    public NotificationControl(Context context, PendingIntent openIntent) {
        this.openIntent = openIntent;
        mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        registerFilter(ACTION_NEXT_TRACK);
        registerFilter(ACTION_PREVIOUS_TRACK);
        registerFilter(ACTION_PAUSE);
        registerFilter(ACTION_PLAY);
        registerFilter(ACTION_DELETE);

    }

    protected void registerFilter(final String filter) {
        filterActions.add(filter);
    }

    private Notification createNotification(@NonNull final AudioTrack track) {
        android.support.v4.app.NotificationCompat.Builder builder = new NotificationCompat.Builder(audioPlayer.getService())
                .setStyle(new NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2))
                .setContentTitle(track.getName())
                .setContentText(track.getPerformer())
                .setSmallIcon(R.drawable.ic_launcher)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

                .setDeleteIntent(createBroadcastIntent(audioPlayer.getService(), ACTION_DELETE, REQUEST_CODE))
                .setContentIntent(openIntent);

        for (final NotificationCompat.Action action : getNotificationActions()) {
            builder.addAction(action);
        }

        return builder.build();
    }

    @Override
    public void onDataChange(boolean hasData) {
        if (audioPlayerAttached) {
            if (hasData) {
                if (audioPlayer.getTrack() != null) {
                    startNotification();
                }
            } else {
                stopNotification();
            }
        }
    }

    public static PendingIntent createBroadcastIntent(final Context context, final String action, final int reqCode) {
        final Intent intent = new Intent()
                .setAction(action)
                .setPackage(context.getApplicationContext().getPackageName());
        return PendingIntent.getBroadcast(context, reqCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    protected ArrayList<NotificationCompat.Action> getNotificationActions() {
        final ArrayList<NotificationCompat.Action> actions = new ArrayList<>();

        actions.add(new NotificationCompat.Action(android.R.drawable.ic_media_previous, "previous", createBroadcastIntent(audioPlayer.getService(), ACTION_PREVIOUS_TRACK, REQUEST_CODE)));

        if (audioPlayerAttached && audioPlayer.isAutoPlay()) {
            actions.add(new NotificationCompat.Action(android.R.drawable.ic_media_pause, "pause", createBroadcastIntent(audioPlayer.getService(), ACTION_PAUSE, REQUEST_CODE)));
        } else {
            actions.add(new NotificationCompat.Action(android.R.drawable.ic_media_play, "play", createBroadcastIntent(audioPlayer.getService(), ACTION_PLAY, REQUEST_CODE)));
        }

        actions.add(new NotificationCompat.Action(android.R.drawable.ic_media_next, "next", createBroadcastIntent(audioPlayer.getService(), ACTION_NEXT_TRACK, REQUEST_CODE)));

        return actions;
    }

    @Override
    public void onTrackChange(AudioTrack track) {
        if (audioPlayerAttached && track != null) {
            updateNotification();
        }
    }

    @Override
    public void onAutoPlayChange(boolean autoplay) {
        if (audioPlayerAttached && audioPlayer.getTrack() != null) {
            if (autoplay) {
                startNotification();
            } else {
                makeDismissable();
            }
        }
    }

    private void startNotification() {
        if (audioPlayerAttached && (!mStarted || mDismissable)) {
            mDismissable = false;

            // The notification must be updated after setting started to true
            final Notification notification = createNotification(audioPlayer.getTrack());
            if (notification != null) {

                if (!mStarted) {
                    final IntentFilter filter = new IntentFilter();
                    for(String filterAction : filterActions) {
                        filter.addAction(filterAction);
                    }
                    audioPlayer.getService().registerReceiver(broadcastReceiver, filter);
                }

                audioPlayer.getService().startForeground(NOTIFICATION_ID, notification);
                mStarted = true;
            }
        } else {
            updateNotification();
        }
    }

    private void makeDismissable() {
        mDismissable = true;

        //on api levels below lollipop calling stopForeground(false) doesn't
        //allow the notification to be removable. So we have to call
        //stopForeground(true) which removes it, and then we add it again...
        //a bit of an eye sore
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            audioPlayer.getService().stopForeground(false);
        } else {
            audioPlayer.getService().stopForeground(true);
        }


        final Notification notification = createNotification(audioPlayer.getTrack());
        if (notification != null) {
            mNotificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    private void stopNotification() {
        if (mStarted && audioPlayerAttached) {
            mStarted = false;

            try {
                mNotificationManager.cancel(NOTIFICATION_ID);
            } catch (IllegalArgumentException ex) {
                // ignore if the receiver is not registered.
            }
            audioPlayer.getService().unregisterReceiver(broadcastReceiver);
            audioPlayer.getService().stopForeground(true);
        }
    }

    public void updateNotification() {
        if (audioPlayerAttached) {
            final Notification notification = createNotification(audioPlayer.getTrack());
            if (mStarted && notification != null) {
                mNotificationManager.notify(NOTIFICATION_ID, notification);
            }
        }
    }

    final private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (audioPlayerAttached) {
                final String action = intent.getAction();
                parseBroadcast(action);
            }
        }
    };

    protected void parseBroadcast(String action) {
        switch (action) {
            case ACTION_PAUSE:
                Log.d("NotificationControl", "received pause request");
                audioPlayer.pause();
                break;
            case ACTION_PLAY:
                Log.d("NotificationControl", "received play request");
                audioPlayer.play();
                break;
            case ACTION_NEXT_TRACK:
                Log.d("NotificationControl", "received next request");
                audioPlayer.nextTrack();
                break;
            case ACTION_PREVIOUS_TRACK:
                Log.d("NotificationControl", "received previous request");
                audioPlayer.previousTrack();
                break;
            case ACTION_DELETE:
                Log.d("NotificationControl", "received delete request");
                audioPlayer.stop();
                break;
            default:
                Log.w("NotificationControl", "Unknown intent ignored. Action=" + action);
        }
    }
}
