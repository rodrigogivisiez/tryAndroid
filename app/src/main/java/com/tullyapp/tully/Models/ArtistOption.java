package com.tullyapp.tully.Models;

/**
 * Created by macbookpro on 10/09/17.
 */

public class ArtistOption {
    public int artist_option_icon;
    public String artist_option_text;

    public ArtistOption(int artist_option_icon, String artist_option_text) {
        this.artist_option_icon = artist_option_icon;
        this.artist_option_text = artist_option_text;
    }

    public int getArtist_option_icon() {
        return artist_option_icon;
    }

    public String getArtist_option_text() {
        return artist_option_text;
    }
}
