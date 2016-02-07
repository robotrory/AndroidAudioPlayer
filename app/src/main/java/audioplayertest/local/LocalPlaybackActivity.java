package audioplayertest.local;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.smithyproductions.audioplayer.AudioTrack;
import com.smithyproductions.audioplayer.Turntable;
import com.smithyproductions.audioplayer.controls.MediaSessionControl;
import com.smithyproductions.audioplayer.controls.NotificationControl;
import com.smithyproductions.audioplayertest.R;

import java.util.ArrayList;

import audioplayertest.BasePlayerActivity;

/**
 * Created by rory on 07/02/16.
 */
public class LocalPlaybackActivity extends BasePlayerActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mExtraFunctionButton.setVisibility(View.GONE);
    }

    @Override
    protected void setAudioPlayer(Turntable turntable) {
        super.setAudioPlayer(turntable);

        if (turntable.getTrackProvider() == null || !(turntable.getTrackProvider() instanceof LocalTrackProvider)) {
            ArrayList<AudioTrack> trackList = new ArrayList<AudioTrack>() {{
                add(AudioTrack.create("Careless Expansion", "JukeDeck", R.raw.careless_expansion, 0));
                add(AudioTrack.create("Enviable Paths", "JukeDeck", R.raw.enviable_paths, 1));
                add(AudioTrack.create("Luxuriant Roads", "JukeDeck", R.raw.luxuriant_roads, 2));
                add(AudioTrack.create("Paris Spheres", "JukeDeck", R.raw.paris_spheres, 3));
            }};

            final LocalTrackProvider trackProvider = new LocalTrackProvider(trackList);
            turntable.setTrackProvider(trackProvider);

            turntable.attachControl(MediaSessionControl.getInstance(getApplicationContext()));
        }


        final NotificationControl notificationControl = NotificationControl.getInstance(this);
        notificationControl.setPendingIntent(PendingIntent.getActivity(this, 0, new Intent(this, LocalPlaybackActivity.class), 0));
        turntable.attachControl(notificationControl);

    }
}
