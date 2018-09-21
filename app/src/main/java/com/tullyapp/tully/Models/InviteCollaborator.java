package com.tullyapp.tully.Models;

/**
 * Created by Santosh on 10/9/18.
 */
public class InviteCollaborator {

    private String sender_id;
    private String receiver_id;
    private String project_id;
    private String sender_name;
    private boolean invite_accept;

    public InviteCollaborator(boolean invite_accept) {
        this.invite_accept = invite_accept;
    }

    public InviteCollaborator(String sender_id, String sender_name, String receiver_id, String project_id, boolean invite_accept) {
        this.sender_id = sender_id;
        this.sender_name = sender_name;
        this.receiver_id = receiver_id;
        this.project_id = project_id;
        this.invite_accept = invite_accept;
    }

    public String getSender_id() {
        return sender_id;
    }

    public void setSender_id(String sender_id) {
        this.sender_id = sender_id;
    }

    public String getSender_name() {
        return sender_name;
    }

    public void setSender_name(String sender_name) {
        this.sender_name = sender_name;
    }

    public String getReceiver_id() {
        return receiver_id;
    }

    public void setReceiver_id(String receiver_id) {
        this.receiver_id = receiver_id;
    }

    public String getProject_id() {
        return project_id;
    }

    public void setProject_id(String project_id) {
        this.project_id = project_id;
    }

    public boolean isInvite_accept() {
        return invite_accept;
    }

    public void setInvite_accept(boolean invite_accept) {
        this.invite_accept = invite_accept;
    }
}