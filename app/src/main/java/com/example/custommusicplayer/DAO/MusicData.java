package com.example.custommusicplayer.DAO;

import java.io.Serializable;

// Adpater의 객체로 사용되는 클래스입니다
public class MusicData implements Serializable {
    private long id;
    private String title;
    private String artist;
    private String album;
    private long albumId;
    private long duration;
    private String data;

    public MusicData(String id, String title, String artist, String album, String albumId, String duration, String data) {
        this.id = Long.parseLong(id);;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.albumId = Long.parseLong(albumId);
        this.duration = Long.parseLong(duration);
        this.data = data;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public Long getAlbumId() {
        return albumId;
    }

    public Long getDuration() {
        return duration;
    }

    public String getData() {
        return data;
    }
}
