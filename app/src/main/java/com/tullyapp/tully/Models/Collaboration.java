package com.tullyapp.tully.Models;

/**
 * Created by Santosh Patil on 7/9/18.
 */
public class Collaboration {

    private String userId;
    private String projectId;
    private String collaborationId;

    public Collaboration(String userId, String projectId, String collaborationId) {
        this.userId = userId;
        this.projectId = projectId;
        this.collaborationId = collaborationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getCollaborationId() {
        return collaborationId;
    }

    public void setCollaborationId(String collaborationId) {
        this.collaborationId = collaborationId;
    }
}