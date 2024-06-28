package com.checkmk.pdctLifeCycle.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class HostNotification {

    @Id
    private String notificationId;

    private String type;
    private String message;
    private LocalDateTime createdAt;
    private String severity;

    //Constructor

    public HostNotification() {
    }

    public HostNotification(String type, String message, String severity) {
        this.type = type;
        this.message = message;
        this.severity = severity;
    }

    //Getters and Setters


    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
}
