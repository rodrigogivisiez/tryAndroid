package com.tullyapp.tully.FirebaseDataModels;

import com.google.firebase.database.Exclude;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by macbookpro on 04/09/17.
 */

public class HomeDb implements Serializable {

    private static final String TAG = HomeDb.class.getSimpleName();
    private HashMap<String,Project> projects;
    private HashMap<String,AudioFile> copytotully;
    private HashMap<String,BeatAudio> beats;

    public HashMap<String, AudioFile> getCopytotully() {
        return copytotully;
    }

    public TreeMap<String, AudioFile> getSortedCopytotully() {
        if (copytotully!=null){
            TreeMap<String, AudioFile> treeMap = new TreeMap<>(copytotully);
            return treeMap;
        }
        return null;
    }

    public TreeMap<String, AudioFile> getSortedAudioFiles() {
        TreeMap<String, AudioFile> treeMap = new TreeMap<>();
        if (copytotully!=null){
            treeMap.putAll(copytotully);
        }
        if (beats!=null){
            for(Object o : beats.entrySet()){
                Map.Entry pair = (Map.Entry) o;
                AudioFile a = (AudioFile) pair.getValue();
                a.setBeat(true);
                treeMap.put(pair.getKey().toString(),a);
            }
        }

        if (treeMap.size()>0) return treeMap;
        return null;
    }

    public HashMap<String, BeatAudio> getBeats() {
        return beats;
    }

    public void setBeats(HashMap<String, BeatAudio> beats) {
        this.beats = beats;
    }

    @Exclude
    private TreeMap<String,Masters> mastersTreeMap;

    @Exclude
    public TreeMap<String, Masters> getMastersTreeMap() {
        return mastersTreeMap;
    }

    @Exclude
    public void setMastersTreeMap(TreeMap<String, Masters> mastersTreeMap) {
        this.mastersTreeMap = mastersTreeMap;
    }

    public void setCopytotully(HashMap<String, AudioFile> copytotully) {
        this.copytotully = copytotully;
    }

    public HashMap<String, Project> getProjects() {
        return projects;
    }

    public TreeMap<String, Project> getSortedProjects(){
        if (projects!=null){
            TreeMap<String, Project> treeMap = new TreeMap<>(projects);
            return treeMap;
        }
        return null;
    }

    public void setProjects(HashMap<String, Project> projects) {
        this.projects = projects;
    }

    public boolean hasAnyFilesOrProjects(){

        if (this.projects!=null && this.projects.size() > 0) return true;

        if (this.copytotully!=null && this.copytotully.size()>0) return true;

        return this.mastersTreeMap != null && this.mastersTreeMap.size() > 0;

    }

    public HomeDb() {
    }
}
