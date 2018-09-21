package com.tullyapp.tully.FirebaseDataModels;

import java.io.Serializable;

public class AudioAnalyzer implements Serializable {
    private boolean isActive;
    private String reason;
    private String subscriptionId;

    public AudioAnalyzer() {
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }
}
