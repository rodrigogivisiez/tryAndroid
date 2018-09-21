package com.tullyapp.tully.FirebaseDataModels.LyricsModule;

import com.tullyapp.tully.FirebaseDataModels.Lyrics;

import java.io.Serializable;

/**
 * Created by macbookpro on 12/09/17.
 */

public class LyricsAppModel implements Serializable{

    private Lyrics lyrics;
    private boolean isChecked = false;
    private boolean isOfProject = false;
    private String projectName;
    private String projectId;

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Lyrics getLyrics() {
        return lyrics;
    }

    public void setLyrics(Lyrics lyrics) {
        this.lyrics = lyrics;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public boolean isOfProject() {
        return isOfProject;
    }

    public void setOfProject(boolean ofProject) {
        isOfProject = ofProject;
    }
}
