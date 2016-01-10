package com.smithyproductions.audioplayer;

/**
 * Created by rory on 07/01/16.
 */
public class AudioTrack {

    private String name;
    private String performer;
    private String url;
    private Integer id;

    public static AudioTrack create(String name, String performer, String url, int id) {
        final AudioTrack audioTrack = new AudioTrack();

        audioTrack.setName(name);
        audioTrack.setPerformer(performer);
        audioTrack.setUrl(url);
        audioTrack.setId(id);

        return audioTrack;
    }


    public void setName(String name) {
        this.name = name;
    }

    public void setPerformer(String performer) {
        this.performer = performer;
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
                ", performer='" + performer + '\'' +
                ", url='" + url + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public String getPerformer() {
        return performer;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
