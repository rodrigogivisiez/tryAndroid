package com.tullyapp.tully.FirebaseDataModels;

import com.google.firebase.database.Exclude;

import java.io.Serializable;

/**
 * Created by macbookpro on 06/09/17.
 */

public class AudioFile implements Serializable {
    @Exclude
    public String id;
    public String filename;
    public String title;
    public String downloadURL;
    public long size;
    public int bpm;
    public String key;

    public float getBpm() {
        return bpm;
    }

    public void setBpm(int bpm) {
        this.bpm = bpm;
    }

    public String getKey() {
        return (key == null ? "" : key);
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Exclude
    private boolean isBeat = false;

    public boolean isBeat() {
        return isBeat;
    }

    public void setBeat(boolean beat) {
        isBeat = beat;
    }

    @Exclude
    public boolean isVideo = false;

    @Exclude
    public boolean isVideo() {
        return isVideo;
    }

    @Exclude
    public void setVideo(boolean video) {
        isVideo = video;
    }

    @Exclude
    public boolean isChecked = false;
    @Exclude
    public String projectId;
    @Exclude
    public String projectName;

    public AudioFile() {
    }

    @Exclude
    public String getDownloadURL() {
        return downloadURL;
    }

    @Exclude
    public String getProjectId() {
        return projectId;
    }

    @Exclude
    public String getProjectName() {
        return projectName;
    }

    @Exclude
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public void setDownloadURL(String downloadURL) {
        this.downloadURL = downloadURL;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Exclude
    public boolean isChecked() {
        return isChecked;
    }

    @Exclude
    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    @Exclude
    public String getId() {
        return id;
    }

    @Exclude
    public void setId(String id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getTitle() {
        return (title==null ? "" : title);
    }

    public void setTitle(String title) {
        this.title = title;
    }

}
