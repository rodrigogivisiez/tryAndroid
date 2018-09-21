package com.tullyapp.tully.Models;

import java.util.Date;

/**
 * Created by Santosh on 6/9/18.
 */
public class ChatMessage {

    private String messageUserId;
    private String messageUser;
    private String messageText;
    private String fileURL;
    private long messageTime;

    public ChatMessage() {

    }

    public ChatMessage(String messageUserId, String messageUser, String messageText, String fileURL) {
        this.messageUserId = messageUserId;
        this.messageUser = messageUser;
        this.messageText = messageText;
        this.fileURL = fileURL;

        // Initialize to current time
        messageTime = new Date().getTime();
    }

    public String getMessageUserId() {
        return messageUserId;
    }

    public void setMessageUserId(String messageUserId) {
        this.messageUserId = messageUserId;
    }

    public String getMessageUser() {
        return messageUser;
    }

    public void setMessageUser(String messageUser) {
        this.messageUser = messageUser;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public String getFileURL() {
        return fileURL;
    }

    public void setFileURL(String fileURL) {
        this.fileURL = fileURL;
    }

    public long getMessageTime() {
        return messageTime;
    }

    public void setMessageTime(long messageTime) {
        this.messageTime = messageTime;
    }
}