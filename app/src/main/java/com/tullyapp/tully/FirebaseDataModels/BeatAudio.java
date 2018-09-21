package com.tullyapp.tully.FirebaseDataModels;

import java.io.Serializable;

/**
 * Created by kathan on 20/03/18.
 */

public class BeatAudio extends AudioFile implements Serializable {

    private String price;

    public BeatAudio() {
        super();
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }
}
