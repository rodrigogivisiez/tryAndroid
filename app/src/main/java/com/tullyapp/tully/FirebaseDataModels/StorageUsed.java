package com.tullyapp.tully.FirebaseDataModels;

import java.io.Serializable;

public class StorageUsed implements Serializable {
    private long masters;

    public StorageUsed(){
    }

    public long getMasters() {
        return masters;
    }

    public void setMasters(long masters) {
        this.masters = masters;
    }
}
