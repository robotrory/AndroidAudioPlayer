package com.smithyproductions.audioplayer.audioEngines;

/**
 * Created by rory on 09/01/16.
 */
public class FadingAudioEngine extends PreloadingAudioEngine {


    private static final int FADE_DURATION = 5000;
    private boolean currentlyFading;

    @Override
    public void onProgress(float progress) {
        if(!playerArray[0].isPreparing()) {
            int timeRemaining = (int) ((1 - progress) * playerArray[0].getDuration());

            if (timeRemaining < FADE_DURATION) {
                if (!playerArray[1].isPreparing()) {
                    currentlyFading = true;
                    float fadeProgress = timeRemaining / (float) FADE_DURATION;
                    playerArray[0].setVolume(fadeProgress);
                    playerArray[1].setVolume(1 - fadeProgress);
                    playerArray[1].play();
                }
            }
        }

        super.onProgress(progress);
    }

    @Override
    public void onTrackFinished() {
        movePlayersToNextTrack();
        playerArray[0].play();
        playerArray[1].setVolume(1);
        currentlyFading = false;
    }

    @Override
    public void pause() {
        if(currentlyFading) {
            swapEngines();
            trackProvider.incrementTrackIndex();
            loadCurrentTracks();
            playerArray[0].setVolume(1);
            playerArray[1].setVolume(1);
            currentlyFading = false;
            parentCallbacks.onProgress(playerArray[0].getProgress());
        }
        super.pause();
    }
}