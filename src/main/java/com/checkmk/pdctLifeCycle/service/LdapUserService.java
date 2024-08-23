package com.checkmk.pdctLifeCycle.service;

import com.checkmk.pdctLifeCycle.model.LdapUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LdapUserService  implements UserDetailsService {


    @Autowired
    private LdapTemplate ldapTemplate;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Query LDAP for the user by email (userPrincipalName)
        LdapQuery query = LdapQueryBuilder.query()
                .where("objectClass").is("person")
                .and("userPrincipalName").is(username);

        // Search for the user in LDAP
        List<LdapUser> users = ldapTemplate.search(query, new UserAttributesMapper());

        // Throw exception if user is not found
        if (users.isEmpty()) {
            throw new UsernameNotFoundException("User not found in LDAP: " + username);
        }

        // Return the first matching user
        return users.get(0);
    }

    public List<LdapUser> getAllUsers() {
        ldapTemplate.setIgnorePartialResultException(true);

        LdapQuery query = LdapQueryBuilder.query()
                .where("objectClass").is("person");
        List<LdapUser> users = ldapTemplate.search(query, new UserAttributesMapper());
        return users.stream()
                .filter(user -> user.getFirstName() != null && user.getLastName() != null && user.getEmail() != null)
                .collect(Collectors.toList());
    }

    // Mapper to convert LDAP attributes to LdapUser objects
    private static class UserAttributesMapper implements org.springframework.ldap.core.AttributesMapper<LdapUser> {
        @Override
        public LdapUser mapFromAttributes(javax.naming.directory.Attributes attributes) throws javax.naming.NamingException {
            // Extract attributes from LDAP entry
            String firstName = attributes.get("givenName") != null ? attributes.get("givenName").get().toString() : null;
            String lastName = attributes.get("sn") != null ? attributes.get("sn").get().toString() : null;
            String email = attributes.get("userPrincipalName") != null ? attributes.get("userPrincipalName").get().toString() : null;

            // Return a new LdapUser with empty authorities (authorities will be handled by CustomLdapUserDetailsContextMapper)
            return new LdapUser(firstName, lastName, email, List.of());
        }
    }

    public LdapUser findUserByEmail(String email) {
        LdapQuery query = LdapQueryBuilder.query()
                .where("objectClass").is("person")
                .and("userPrincipalName").is(email);

        List<LdapUser> users = ldapTemplate.search(query, new UserAttributesMapper());

        return users.isEmpty() ? null : users.get(0); // Return the first match or null if not found
    }



}
