package com.checkmk.pdctLifeCycle.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class HostNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String message;

    private boolean read;

    private String createdAt;

    // Store the LDAP username instead of a HostUser entity
    private String hostUserEmail;

    // Constructors, getters, and setters

    public HostNotification(String title, String message, String hostUserEmail) {
        this.title = title;
        this.message = message;
        this.hostUserEmail = hostUserEmail;
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
}
