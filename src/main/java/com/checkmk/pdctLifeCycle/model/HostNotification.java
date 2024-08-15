package com.checkmk.pdctLifeCycle.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class HostNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String message;

    private boolean read;

    private LocalDateTime createdAt;

    @ManyToOne
    private HostUser user;

    // Constructors, getters, setters

    public HostNotification() {
        this.createdAt = LocalDateTime.now();
        this.read = false;
    }

    //Getters and Setters


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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public HostUser getUser() {
        return user;
    }

    public void setUser(HostUser user) {
        this.user = user;
    }
}
