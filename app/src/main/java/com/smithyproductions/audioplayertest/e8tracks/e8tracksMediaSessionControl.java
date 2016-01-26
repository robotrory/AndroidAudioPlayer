package com.smithyproductions.audioplayertest.e8tracks;

import android.content.Context;

import com.smithyproductions.audioplayer.controls.MediaSessionControl;

/**
 * Created by rory on 22/01/16.
 */
public class e8tracksMediaSessionControl extends MediaSessionControl {

    public e8tracksMediaSessionControl(Context context) {
        super(context);
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
