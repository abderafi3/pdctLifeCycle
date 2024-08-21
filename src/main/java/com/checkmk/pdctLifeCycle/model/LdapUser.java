package com.checkmk.pdctLifeCycle.model;

public class LdapUser {
    private String firstName;
    private String lastName;
    private String email;

    public LdapUser(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    // Getters
    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }
}
