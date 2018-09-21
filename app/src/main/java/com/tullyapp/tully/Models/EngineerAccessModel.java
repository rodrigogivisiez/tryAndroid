package com.tullyapp.tully.Models;

import com.google.firebase.database.Exclude;

import java.io.Serializable;

/**
 * Created by apple on 23/01/18.
 */

public class EngineerAccessModel implements Serializable {
    private String id;
    private String email;
    private String name = "";
    private Boolean adminAccess = false;

    // for firebase
    public EngineerAccessModel() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getAdminAccess() {
        return adminAccess;
    }

    public void setAdminAccess(Boolean adminAccess) {
        this.adminAccess = adminAccess;
    }

}
