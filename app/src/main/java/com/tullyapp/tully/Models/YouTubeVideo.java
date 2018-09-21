package com.tullyapp.tully.Models;

/**
 * Created by apple on 07/12/17.
 */

public class YouTubeVideo {

    private String id;
    private String title;
    private String description;
    private long time;
    private String thumbnail;

    public YouTubeVideo(String id, String title, String description, long time, String thumbnail) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.time = time;
        this.thumbnail = thumbnail;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public long getTime() {
        return time;
    }

    public String getThumbnail() {
        return thumbnail;
    }
}
