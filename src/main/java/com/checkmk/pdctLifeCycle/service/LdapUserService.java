package com.checkmk.pdctLifeCycle.service;

import com.checkmk.pdctLifeCycle.model.LdapUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LdapUserService {
    @Autowired
    private LdapTemplate ldapTemplate;

    private static class UserContextMapper implements ContextMapper<LdapUser> {
        @Override
        public LdapUser mapFromContext(Object ctx) throws javax.naming.NamingException {
            DirContextOperations context = (DirContextOperations) ctx;

            String firstName = context.getStringAttribute("givenName");
            String lastName = context.getStringAttribute("sn");
            String email = context.getStringAttribute("userPrincipalName");
            return new LdapUser(firstName, lastName, email, List.of());
        }
    }

    public List<LdapUser> getAllUsers() {
        ldapTemplate.setIgnorePartialResultException(true);

        LdapQuery query = LdapQueryBuilder.query()
                .where("objectClass").is("person");

        List<LdapUser> users = ldapTemplate.search(query, new UserContextMapper());

        return users.stream()
                .filter(user -> user.getFirstName() != null && user.getLastName() != null && user.getEmail() != null)
                .collect(Collectors.toList());
    }

    public LdapUser findUserByEmail(String email) {
        LdapQuery query = LdapQueryBuilder.query()
                .where("objectClass").is("person")
                .and("userPrincipalName").is(email);

        List<LdapUser> users = ldapTemplate.search(query, new UserContextMapper());

        return users.isEmpty() ? null : users.get(0);
    }

}
