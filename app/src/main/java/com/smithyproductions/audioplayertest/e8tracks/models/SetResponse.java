package com.smithyproductions.audioplayertest.e8tracks.models;

/**
 * Created by rory on 09/01/16.
 */
public class SetResponse {
    public boolean at_beginning;
    public boolean at_end;
    public boolean at_last_track;
    public boolean skip_allowed;
    public TrackResponse track;
    public TrackResponse next_track;
}
