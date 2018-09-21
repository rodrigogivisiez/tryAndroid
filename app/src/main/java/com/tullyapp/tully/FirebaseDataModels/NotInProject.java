package com.tullyapp.tully.FirebaseDataModels;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by macbookpro on 04/09/17.
 */

public class NotInProject implements Serializable {

    public HashMap<String,Lyrics> lyrics;
    public HashMap<String,Recording> recordings;

    public HashMap<String, Lyrics> getLyrics() {
        return lyrics;
    }

    public void setLyrics(HashMap<String, Lyrics> lyrics) {
        this.lyrics = lyrics;
    }

    public HashMap<String, Recording> getRecordings() {
        return recordings;
    }

    public void setRecordings(HashMap<String, Recording> recordings) {
        this.recordings = recordings;
    }

    public NotInProject() {
    }
}
