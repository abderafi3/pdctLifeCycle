package com.checkmk.pdctLifeCycle.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class LdapUser implements UserDetails {
    private String firstName;
    private String lastName;
    private String email;
    private Collection<? extends GrantedAuthority> authorities;

    public LdapUser(String firstName, String lastName, String email, Collection<? extends GrantedAuthority> authorities) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.authorities = authorities;
    }

    // Getter methods for firstName, lastName, and email
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;  // LDAP typically doesn't provide passwords for authentication
    }

    @Override
    public String getUsername() {
        return email;  // Use email as the username
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
