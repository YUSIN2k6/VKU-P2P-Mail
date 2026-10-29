package com.vku.app.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MailMessage {
    private String sendingTime;
    private String sender;
    private String receiver;
    private String title;
    private String content;
    private List<File> files = new ArrayList<>();

    public MailMessage() {
    }

    public MailMessage(String sendingTime, String sender, String receiver, String title, String content) {
        this.sendingTime = sendingTime;
        this.sender = sender;
        this.receiver = receiver;
        this.title = title;
        this.content = content;
    }

    public String getSendingTime() {
        return sendingTime;
    }

    public void setSendingTime(String sendingTime) {
        this.sendingTime = sendingTime;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<File> getFiles() {
        return files;
    }

    public void setFiles(List<File> files) {
        this.files = files;
    }

    public void addFile(File file) {
        if (this.files == null) {
            this.files = new ArrayList<>();
        }
        this.files.add(file);
    }
}

