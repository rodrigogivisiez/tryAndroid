package com.tullyapp.tully.FirebaseDataModels;

import java.io.Serializable;

/**
 * Created by macbookpro on 04/09/17.
 */

public class Profile implements Serializable {
    private String artist_name;
    private String artist_option;
    private String email;
    private String genre;
    private StorageUsed storageUsed;

    public Profile() {
        // For Firebaase
    }

    public Profile(String artist_name, String artist_option, String email, String genre) {
        this.artist_name = artist_name;
        this.artist_option = artist_option;
        this.email = email;
        this.genre = genre;
    }

    public void setArtist_name(String artist_name) {
        this.artist_name = artist_name;
    }

    public void setArtist_option(String artist_option) {
        this.artist_option = artist_option;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getArtist_name() {
        return artist_name;
    }

    public String getArtist_option() {
        return artist_option;
    }

    public String getEmail() {
        return email;
    }

    public String getGenre() {
        return genre;
    }

    public StorageUsed getStorageUsed() {
        return storageUsed;
    }

    public void setStorageUsed(StorageUsed storageUsed) {
        this.storageUsed = storageUsed;
    }
}
