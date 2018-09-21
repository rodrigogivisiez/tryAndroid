package com.tullyapp.tully.FirebaseDataModels;

import com.google.firebase.database.Exclude;

import java.io.Serializable;

/**
 * Created by kathan on 28/01/18.
 */

public class Masters implements Serializable {
    private String name;
    private String parent_id;
    private String type;
    private String downloadURL;
    private String filename;
    private String lyrics;
    private long count;
    private long size;
    private int bpm;
    private String key;
    private String parentEngineer;
    private String engineer_id;

    public String getParentEngineer() {
        return parentEngineer;
    }

    public void setParentEngineer(String parentEngineer) {
        this.parentEngineer = parentEngineer;
    }

    public String getEngineer_id() {
        return engineer_id;
    }

    public void setEngineer_id(String engineer_id) {
        this.engineer_id = engineer_id;
    }

    public int getBpm() {
        return bpm;
    }

    public void setBpm(int bpm) {
        this.bpm = bpm;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Exclude
    private String id;

    public String getId() {
        return id;
    }

    public long getSize() {
        return size;
    }

    public String getLyrics() {
        return lyrics;
    }

    public void setLyrics(String lyrics) {
        this.lyrics = lyrics;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDownloadURL() {
        return downloadURL;
    }

    public void setDownloadURL(String downloadURL) {
        this.downloadURL = downloadURL;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParent_id() {
        return parent_id;
    }

    public void setParent_id(String parent_id) {
        this.parent_id = parent_id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Masters() {
    }
}
