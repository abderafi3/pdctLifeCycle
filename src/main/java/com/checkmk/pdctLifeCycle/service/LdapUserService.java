package com.checkmk.pdctLifeCycle.service;

import com.checkmk.pdctLifeCycle.model.LdapUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.stereotype.Service;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.util.List;

@Service
public class LdapUserService {

    private final LdapContextSource contextSource;

    @Autowired
    public LdapUserService(LdapContextSource contextSource) {
        this.contextSource = contextSource;
    }

    @Autowired
    private LdapTemplate ldapTemplate;

    public List<LdapUser> getAllUsers() {
        // Set referral policy to ignore or follow based on your needs
        ldapTemplate.setIgnorePartialResultException(true);  // Ignore referrals

        // Build the LDAP query for person objects
        LdapQuery query = LdapQueryBuilder.query()
                .where("objectClass").is("person");  // Adjust the object class based on your LDAP structure

        // Perform the search and map the result to LdapUser objects
        return ldapTemplate.search(query, new UserAttributesMapper());
    }

    // Mapper to convert LDAP attributes to LdapUser objects
    private static class UserAttributesMapper implements AttributesMapper<LdapUser> {
        @Override
        public LdapUser mapFromAttributes(Attributes attributes) throws NamingException {
            String firstName = attributes.get("givenName") != null ? attributes.get("givenName").get().toString() : "";
            String lastName = attributes.get("sn") != null ? attributes.get("sn").get().toString() : "";
            String email = attributes.get("mail") != null ? attributes.get("mail").get().toString() : "";
            return new LdapUser(firstName, lastName, email);
        }
    }
}
