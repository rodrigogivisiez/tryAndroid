package com.tullyapp.tully.FirebaseDataModels;

import com.google.firebase.database.Exclude;

import java.io.Serializable;

/**
 * Created by macbookpro on 04/09/17.
 */

public class Recording implements Serializable {

    private String name;
    private String tid;
    private long size;
    private String mime;
    private String downloadURL;
    private long duration;
    private int bpm;
    private String key;

    public float getBpm() {
        return bpm;
    }

    public void setBpm(int bpm) {
        this.bpm = bpm;
    }

    public String getKey() {
        return (key==null ? "---" : key);
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Exclude
    private int volume = 100;

    @Exclude
    public int getVolume() {
        return volume;
    }

    @Exclude
    public void setVolume(int volume) {
        this.volume = volume;
    }

    @Exclude
    private String progress;
    @Exclude
    private boolean showVolume;
    @Exclude
    public boolean isShowVolume() {
        return showVolume;
    }
    @Exclude
    public void setShowVolume(boolean showVolume) {
        this.showVolume = showVolume;
    }

    @Exclude
    public String getProgress() {
        return progress;
    }

    @Exclude
    public void setProgress(String progress) {
        this.progress = progress;
    }

    @Exclude
    private boolean isLocalAvailable = false;
    @Exclude
    private String localPath;

    @Exclude
    private String id;
    @Exclude
    private boolean isPlaySelected = false;
    @Exclude
    private boolean isPlaying = false;
    @Exclude
    private boolean isPaused = false;
    @Exclude
    private String projectName;
    @Exclude
    private String projectId;
    @Exclude
    private boolean checked = false;
    @Exclude
    private boolean isOfProject;
    @Exclude
    private int progressPercent;
    @Exclude
    private int outerPos;

    @Exclude
    public String getLocalPath() {
        return localPath;
    }
    @Exclude
    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    @Exclude
    public boolean isLocalAvailable() {
        return isLocalAvailable;
    }
    @Exclude
    public void setLocalAvailable(boolean localAvailable) {
        isLocalAvailable = localAvailable;
    }

    @Exclude
    public int getOuterPos() {
        return outerPos;
    }
    @Exclude
    public void setOuterPos(int outerPos) {
        this.outerPos = outerPos;
    }

    @Exclude
    public int getProgressPercent() {
        return progressPercent;
    }
    @Exclude
    public void setProgressPercent(int progressPercent) {
        this.progressPercent = progressPercent;
    }


    @Exclude
    public boolean isPaused() {
        return isPaused;
    }

    @Exclude
    public void setPaused(boolean paused) {
        isPaused = paused;
    }

    @Exclude
    public boolean isPlaying() {
        return isPlaying;
    }
    @Exclude
    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    @Exclude
    public boolean isPlaySelected() {return isPlaySelected;}

    @Exclude
    public void setPlaySelected(boolean playSelected) {isPlaySelected = playSelected;}

    @Exclude
    public String getProjectName() {
        return projectName;
    }

    @Exclude
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    @Exclude
    public String getProjectId() {
        return projectId;
    }

    @Exclude
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Exclude
    public boolean isChecked() {
        return checked;
    }

    @Exclude
    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    @Exclude
    public boolean isOfProject() {
        return isOfProject;
    }

    @Exclude
    public void setOfProject(boolean ofProject) {
        isOfProject = ofProject;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getMime() {
        return mime;
    }

    public void setMime(String mime) {
        this.mime = mime;
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

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    @Exclude
    public String getId() {
        return id;
    }

    @Exclude
    public void setId(String id) {
        this.id = id;
    }

    public Recording() {
    }
}
