package com.checkmk.pdctLifeCycle.service;

import com.checkmk.pdctLifeCycle.model.LdapUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.stereotype.Service;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LdapUserService {


    @Autowired
    private LdapTemplate ldapTemplate;

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
    private static class UserAttributesMapper implements AttributesMapper<LdapUser> {
        @Override
        public LdapUser mapFromAttributes(Attributes attributes) throws NamingException {
            String firstName = attributes.get("givenName") != null ? attributes.get("givenName").get().toString() : null;
            String lastName = attributes.get("sn") != null ? attributes.get("sn").get().toString() : null;
            String email = attributes.get("userPrincipalName") != null ? attributes.get("userPrincipalName").get().toString() : null;
            return new LdapUser(firstName, lastName, email);
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
