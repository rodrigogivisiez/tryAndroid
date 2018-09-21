package com.tullyapp.tully.FirebaseDataModels;

import com.google.firebase.database.Exclude;

import java.io.Serializable;

public class EngineerAdminAccess implements Serializable {
    private boolean isActive;
    private String planId;
    private String planType;
    private String subscriptionId;

    private String customer_id;

    public void setCustomer_id(String customer_id) {
        this.customer_id = customer_id;
    }

    public EngineerAdminAccess() { }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getPlanType() {
        return planType;
    }

    public void setPlanType(String planType) {
        this.planType = planType;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getCustomer_id() {
        return customer_id;
    }

}
