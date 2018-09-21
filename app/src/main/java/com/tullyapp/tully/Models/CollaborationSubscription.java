package com.tullyapp.tully.Models;

/**
 * Created by Santosh on 17/9/18.
 */
public class CollaborationSubscription {

    private String from;
    private boolean is_subscribe;

    public CollaborationSubscription(String from, boolean is_subscribe) {
        this.from = from;
        this.is_subscribe = is_subscribe;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public boolean isIs_subscribe() {
        return is_subscribe;
    }

    public void setIs_subscribe(boolean is_subscribe) {
        this.is_subscribe = is_subscribe;
    }
}