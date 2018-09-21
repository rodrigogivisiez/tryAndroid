package com.tullyapp.tully.Models;

/**
 * Created by apple on 26/11/17.
 */

public class RemainingUpload {
    private int _id;
    private String upload_type;
    private String file_path;
    private String data;
    private String key;

    public RemainingUpload(int _id, String upload_type, String file_path, String data) {
        this._id = _id;
        this.upload_type = upload_type;
        this.file_path = file_path;
        this.data = data;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int get_id() {
        return _id;
    }

    public String getUpload_type() {
        return upload_type;
    }

    public String getFile_path() {
        return file_path;
    }

    public String getData() {
        return data;
    }
}
