package com.tullyapp.tully.Models;

import com.google.firebase.database.Exclude;

import java.io.Serializable;

/**
 * Created by kathan on 12/02/18.
 */

public class Beats implements Serializable {

    private int id;
    private String name;
    private Double price;
    private String trackURL;
    private String trackName;
    private String producer_name;
    private String email;
    private String type;
    private String genre;
    private long trackSize;
    private boolean free = false;

    @Exclude
    private boolean isPlaySelected = false;
    @Exclude
    private boolean isPlaying = false;
    @Exclude
    private boolean isPaused = false;

    public Beats(int id, String name, Double price, String trackURL, String trackName, String producer_name, String email, long trackSize, String type, String genre, boolean free) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.trackURL = trackURL;
        this.trackName = trackName;
        this.producer_name = producer_name;
        this.email = email;
        this.trackSize = trackSize;
        this.type = type;
        this.genre = genre;
        this.free = free;
    }

    public boolean isFree() {
        return free;
    }

    public String getType() {
        return type;
    }

    public String getGenre() {
        return genre;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Double getPrice() {
        return price;
    }

    public String getTrackURL() {
        return trackURL;
    }

    public String getTrackName() {
        return trackName;
    }

    public String getProducer_name() {
        return producer_name;
    }

    public String getEmail() {
        return email;
    }

    public long getTrackSize() {
        return trackSize;
    }

    public boolean isPlaySelected() {
        return isPlaySelected;
    }

    public void setPlaySelected(boolean playSelected) {
        isPlaySelected = playSelected;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        isPaused = paused;
    }
}
