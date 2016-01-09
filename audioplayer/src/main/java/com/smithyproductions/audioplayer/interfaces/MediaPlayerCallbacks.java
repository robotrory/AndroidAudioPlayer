package com.smithyproductions.audioplayer.interfaces;

/**
 * Created by rory on 07/01/16.
 */
public interface MediaPlayerCallbacks {
    void onTrackFinished();
    void onProgress(float progress);
    void onError();
}
