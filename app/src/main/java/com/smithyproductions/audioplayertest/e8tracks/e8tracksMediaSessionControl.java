package com.smithyproductions.audioplayertest.e8tracks;

import com.smithyproductions.audioplayer.controls.MediaSessionControl;

/**
 * Created by rory on 22/01/16.
 */
public class e8tracksMediaSessionControl extends MediaSessionControl {

    @Override
    protected void handleNext() {
        if(audioPlayerAttached) {
            if (audioPlayer.getTrackProvider() != null && audioPlayer.getTrackProvider() instanceof MixSetTrackProvider && !((MixSetTrackProvider) audioPlayer.getTrackProvider()).canSkip()) {
                ((MixSetTrackProvider)audioPlayer.getTrackProvider()).goToNextMix();
            } else {
                audioPlayer.nextTrack();
            }
        }
    }
}
