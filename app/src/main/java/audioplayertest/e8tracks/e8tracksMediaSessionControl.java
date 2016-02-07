package audioplayertest.e8tracks;

import android.content.Context;

import com.smithyproductions.audioplayer.controls.MediaSessionControl;

/**
 * Created by rory on 22/01/16.
 */
public class e8tracksMediaSessionControl extends MediaSessionControl {

    private static e8tracksMediaSessionControl sInstance;

    private e8tracksMediaSessionControl(Context context) {
        super(context);
    }

    public static e8tracksMediaSessionControl getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new e8tracksMediaSessionControl(context);
        }
        return sInstance;
    }

    @Override
    protected void handleNext() {
        if(audioPlayerAttached) {
            if (turntable.getTrackProvider() != null && turntable.getTrackProvider() instanceof MixSetTrackProvider && !((MixSetTrackProvider) turntable.getTrackProvider()).canSkip()) {
                ((MixSetTrackProvider) turntable.getTrackProvider()).goToNextMix();
            } else {
                turntable.nextTrack();
            }
        }
    }
}
