package com.smithyproductions.audioplayer;

/**
 * Created by rory on 07/01/16.
 */
public class AudioTrack {

    private String name;
    private String artist;
    private String url;
    private int localResId;
    private Integer id;
    private String artworkUrl;

    private AudioTrack () {

    }

    public static AudioTrack create(String name, String performer, String url, int id) {
        final AudioTrack audioTrack = new AudioTrack();

        audioTrack.setName(name);
        audioTrack.setArtist(performer);
        audioTrack.setUrl(url);
        audioTrack.setId(id);

        return audioTrack;
    }

    public static AudioTrack create(String name, String performer, int resId, int id) {
        final AudioTrack audioTrack = new AudioTrack();

        audioTrack.setName(name);
        audioTrack.setArtist(performer);
        audioTrack.setUrl("raw://"+String.valueOf(resId));
        audioTrack.setLocalResId(resId);
        audioTrack.setId(id);

        return audioTrack;
    }


    public void setName(String name) {
        this.name = name;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return "AudioTrack{" +
                "name='" + name + '\'' +
                ", artist='" + artist + '\'' +
                ", url='" + url + '\'' +
                ", id=" + id +
                ", artworkUrl='" + artworkUrl + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public String getArtist() {
        return artist;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getArtworkUrl() {
        return artworkUrl;
    }

    public void setArtworkUrl(String artworkUrl) {
        this.artworkUrl = artworkUrl;
    }

    public int getLocalResId() {
        return localResId;
    }

    public void setLocalResId(int localResId) {
        this.localResId = localResId;
    }
}
