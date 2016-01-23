package com.smithyproductions.audioplayertest.e8tracks.models;

/**
 * Created by rory on 09/01/16.
 */
public class MixResponse {
    public String name;
    public Integer id;
    public String web_path;
    public CoverUrlResponse cover_urls;

    @Override
    public String toString() {
        return "MixResponse{" +
                "name='" + name + '\'' +
                ", id=" + id +
                '}';
    }
}
