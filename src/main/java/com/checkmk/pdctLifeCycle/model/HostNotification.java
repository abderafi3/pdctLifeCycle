package com.checkmk.pdctLifeCycle.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class HostNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 1024)
    private String message;
    private boolean read;
    private String createdAt;
    private String hostUserEmail;
    private String createdBy;
    private String hostName;
    private String userFullName;

    // Constructors

    public HostNotification(String title, String message, String hostUserEmail, String createdBy, String hostName, String userFullName) {
        this.title = title;
        this.message = message;
        this.hostUserEmail = hostUserEmail;
        this.createdBy = createdBy;
        this.hostName = hostName;
        this.userFullName = userFullName;
        this.createdAt = LocalDateTime.now().toString();
        this.read = false;
    }

    public HostNotification() {
        this.createdAt = LocalDateTime.now().toString();
        this.read = false;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getHostUserEmail() {
        return hostUserEmail;
    }

    public void setHostUserEmail(String hostUserEmail) {
        this.hostUserEmail = hostUserEmail;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }
}
