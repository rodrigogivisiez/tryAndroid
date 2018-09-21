package com.tullyapp.tully.FirebaseDataModels;

import com.google.firebase.database.Exclude;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by macbookpro on 04/09/17.
 */

public class Project implements Serializable {

    private String ext;
    private String project_main_recording;
    private String project_name;
    private HashMap<String,Recording> recordings;
    private HashMap<String,Lyrics> lyrics;

    @Exclude
    private String id;

    @Exclude
    public String getId() {
        return id;
    }

    @Exclude
    public void setId(String id) {
        this.id = id;
    }

    private boolean isChecked = false;

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public HashMap<String, Recording> getRecordings() {
        return recordings;
    }

    public void setRecordings(HashMap<String, Recording> recordings) {
        this.recordings = recordings;
    }

    public String getMainFileTitle(){
        if (this.recordings!=null && this.project_main_recording!=null){
            for (Object o : this.recordings.entrySet()) {
                Map.Entry pair = (Map.Entry) o;
                Recording rec = (Recording) pair.getValue();
                if (rec.getTid().equals(this.project_main_recording)) {
                    return rec.getName();
                }
            }
        }
        return "";
    }

    public HashMap<String, Lyrics> getLyrics() {
        return lyrics;
    }

    public void setLyrics(HashMap<String, Lyrics> lyrics) {
        this.lyrics = lyrics;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public String getProject_main_recording() {
        return project_main_recording;
    }

    public void setProject_main_recording(String project_main_recording) {
        this.project_main_recording = project_main_recording;
    }

    @Exclude
    public int getItemcount(){
        int no = 0;

        if (this.recordings!=null)
            no += this.recordings.size();

        if (this.getLyrics()!=null)
            no += this.getLyrics().size();

        return no;

    }

    public String getProject_name() {
        return (project_name == null ? "" : project_name);
    }

    public void setProject_name(String project_name) {
        this.project_name = project_name;
    }

    public Project() {
    }
}
