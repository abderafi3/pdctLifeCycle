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

        // Retrieve department and team (description)
        String department = ctx.getStringAttribute("department");
        String team = ctx.getStringAttribute("description");

        List<GrantedAuthority> mappedAuthorities = new ArrayList<>();

        String[] memberOf = ctx.getStringAttributes("memberOf");

        // Only add ROLE_USER if no specific roles (like Admin, DepartmentHead, TeamLeader) are found
        boolean hasSpecificRole = false;

        if (memberOf != null) {
            for (String group : memberOf) {
                // Convert group membership to roles - normalize and apply mapping logic
                if (group.toLowerCase().contains("administrators")) {
                    mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    hasSpecificRole = true;
                }
                // Correct spelling for DepartmentHead
                if (group.toLowerCase().contains("departmenthead")) {
                    mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_DEPARTMENTHEAD"));
                    hasSpecificRole = true;
                }
                // Correctly map team leader role
                if (group.toLowerCase().contains("teamleader")) {
                    mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_TEAMLEADER"));
                    hasSpecificRole = true;
                }
            }
        }

        // If no specific roles were mapped, add the default user role
        if (!hasSpecificRole) {
            mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        // Add any authorities passed in (if needed)
//        mappedAuthorities.addAll(authorities);

        // Return a custom LdapUser with department and team
        return new LdapUser(firstName, lastName, email, department, team, mappedAuthorities);
    }

    @Override
    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
        throw new UnsupportedOperationException("Operation not supported.");
    }
}
