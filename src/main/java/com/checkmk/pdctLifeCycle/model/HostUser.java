package com.checkmk.pdctLifeCycle.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.util.List;
import java.util.Set;

@Entity
public class HostUser {

    @Id
    private String userId;

    private String firstName;

    private String lastName;

    private String email;

    private String password;

    private String role;

    @OneToMany(mappedBy = "user")
    @JsonManagedReference  // Serialize this side of the relationship
    private List<HostNotification> notifications;

    @OneToMany(mappedBy = "hostUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Host> hosts;

    //Constructor

    public HostUser() {
    }

    public HostUser(String firstName, String lastName, String email, String password, String role) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.role = role;
    }


    //Getters and Setters


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Set<Host> getHosts() {
        return hosts;
    }

    public void setHosts(Set<Host> hosts) {
        this.hosts = hosts;
    }
}
