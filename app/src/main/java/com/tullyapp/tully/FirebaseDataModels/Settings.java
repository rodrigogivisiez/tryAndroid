package com.tullyapp.tully.FirebaseDataModels;

import java.io.Serializable;

/**
 * Created by apple on 01/12/17.
 */

public class Settings implements Serializable {

    private boolean pushNotification;
    private boolean touchId;
    private String customer_id;
    private AudioAnalyzer audioAnalyzer;
    private EngineerAdminAccess engineerAdminAccess;

    public Settings() {}

    public boolean isPushNotification() {
        return pushNotification;
    }

    public void setPushNotification(boolean pushNotification) {
        this.pushNotification = pushNotification;
    }

    public boolean isTouchId() {
        return touchId;
    }

    public void setTouchId(boolean touchId) {
        this.touchId = touchId;
    }

    public String getCustomer_id() {
        return customer_id;
    }
    public AudioAnalyzer getAudioAnalyzer() {
        return audioAnalyzer;
    }

    public void setAudioAnalyzer(AudioAnalyzer audioAnalyzer) {
        this.audioAnalyzer = audioAnalyzer;
    }

    public EngineerAdminAccess getEngineerAdminAccess() {
        return engineerAdminAccess;
    }

    public void setEngineerAdminAccess(EngineerAdminAccess engineerAdminAccess) {
        this.engineerAdminAccess = engineerAdminAccess;
    }
}
