package com.tullyapp.tully.Models;

import org.json.JSONArray;

/**
 * Created by macbookpro on 28/09/17.
 */

public class LyricsWordSynonym {

    public String word;
    public boolean isHighlighted;
    public JSONArray description;

    public LyricsWordSynonym(String word, boolean isHighlighted, JSONArray description) {
        this.word = word;
        this.isHighlighted = isHighlighted;
        this.description = description;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public boolean isHighlighted() {
        return isHighlighted;
    }

    public void setHighlighted(boolean highlighted) {
        isHighlighted = highlighted;
    }

    public JSONArray getDescription() {
        return description;
    }

    public void setDescription(JSONArray description) {
        this.description = description;
    }
}
