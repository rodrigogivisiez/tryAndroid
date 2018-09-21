package com.tullyapp.tully.Models;

import com.tullyapp.tully.FirebaseDataModels.Recording;

import java.util.ArrayList;

/**
 * Created by apple on 06/01/18.
 */

public class OrganizedRec {
    public ArrayList<Recording> recordingList;
    public Recording recording;
    public boolean isOfProject;
    public boolean isExpanded = false;
    public String projectId = "";

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    public OrganizedRec() {
        this.recordingList = new ArrayList<>();
    }

    public Recording getRecording() {
        return recording;
    }

    public void setRecording(Recording recording) {
        this.recording = recording;
    }

    public ArrayList<Recording> getRecordingList() {
        return recordingList;
    }

    public void setRecordingList(ArrayList<Recording> recordingList) {
        this.recordingList = recordingList;
    }

    public boolean isOfProject() {
        return isOfProject;
    }

    public void setOfProject(boolean ofProject) {
        isOfProject = ofProject;
    }
}
