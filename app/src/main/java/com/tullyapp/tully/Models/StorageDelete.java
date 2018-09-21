package com.tullyapp.tully.Models;

/**
 * Created by apple on 30/11/17.
 */

public class StorageDelete {
    private int _id;
    private String storage_path;

    public StorageDelete(int _id, String storage_path) {
        this._id = _id;
        this.storage_path = storage_path;
    }

    public int get_id() {
        return _id;
    }

    public String getStorage_path() {
        return storage_path;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public void setStorage_path(String storage_path) {
        this.storage_path = storage_path;
    }
}
