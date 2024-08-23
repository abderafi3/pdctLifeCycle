package com.checkmk.pdctLifeCycle.model;

import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CustomLdapUserDetailsContextMapper implements UserDetailsContextMapper {

    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username, Collection<? extends GrantedAuthority> authorities) {
        // Extract user details from the LDAP context
        String firstName = ctx.getStringAttribute("givenName");
        String lastName = ctx.getStringAttribute("sn");
        String email = ctx.getStringAttribute("userPrincipalName");

        // Map LDAP group memberships to Spring Security roles
        List<GrantedAuthority> mappedAuthorities = new ArrayList<>();

        // Extract 'memberOf' attribute, which usually contains LDAP group memberships
        String[] memberOf = ctx.getStringAttributes("memberOf");

        if (memberOf != null) {
            for (String group : memberOf) {
                // Convert group membership to roles - normalize and apply mapping logic
                if (group.toLowerCase().contains("administrators")) {
                    mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                } else if (group.toLowerCase().contains("users")) {
                    mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                }
            }
        }

        // Add default role if no specific roles were assigned
        if (mappedAuthorities.isEmpty()) {
            mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        // Add any additional authorities passed from the authentication provider (if any)
        mappedAuthorities.addAll(authorities);

        // Return the custom LdapUser with the mapped authorities
        return new LdapUser(firstName, lastName, email, mappedAuthorities);
    }

    @Override
    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
        // Not needed for LDAP authentication, so throw an exception
        throw new UnsupportedOperationException("Operation not supported.");
    }
}
