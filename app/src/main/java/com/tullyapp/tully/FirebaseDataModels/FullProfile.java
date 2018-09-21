package com.tullyapp.tully.FirebaseDataModels;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by macbookpro on 19/09/17.
 */

public class FullProfile implements Serializable{
    private HashMap<String,Project> projects;
    private HashMap<String,AudioFile> copytotully;
    private NotInProject no_project;
    private Profile profile;

    public HashMap<String, AudioFile> getCopytotully() {
        return copytotully;
    }

    public void setCopytotully(HashMap<String, AudioFile> copytotully) {
        this.copytotully = copytotully;
    }

    public HashMap<String, Project> getProjects() {
        return projects;
    }

    public void setProjects(HashMap<String, Project> projects) {
        this.projects = projects;
    }

    public boolean hasAnyFilesOrProjects(){

        if (this.projects!=null){
            if (this.projects.size() > 0)
                return true;
        }

        if (this.copytotully!=null){
            if (this.copytotully.size()>0)
                return  true;
        }

        return false;
    }

    public NotInProject getNo_project() {
        return no_project;
    }

    public void setNo_project(NotInProject no_project) {
        this.no_project = no_project;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public FullProfile() {
    }
}
