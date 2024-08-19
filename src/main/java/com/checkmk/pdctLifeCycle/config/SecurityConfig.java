package com.checkmk.pdctLifeCycle.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.ldap.urls}")
    private String ldapUrl;

    @Value("${spring.ldap.base}")
    private String ldapBase;

    @Value("${spring.ldap.username}")
    private String ldapUsername;

    @Value("${spring.ldap.password}")
    private String ldapPassword;

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/register", "/error", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/hosts/add", "/hosts/edit/**", "/hosts/delete/**", "/hosts/import").hasRole("ADMIN")
                        .requestMatchers("/hosts").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login").permitAll()
                        .defaultSuccessUrl("/hosts", true)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout").permitAll()
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public ActiveDirectoryLdapAuthenticationProvider activeDirectoryLdapAuthenticationProvider() {
        ActiveDirectoryLdapAuthenticationProvider provider =
                new ActiveDirectoryLdapAuthenticationProvider("asagno.local", "ldap://192.168.114.145:389");
        provider.setConvertSubErrorCodesToExceptions(true);
        provider.setUseAuthenticationRequestCredentials(true);
        provider.setAuthoritiesMapper(grantedAuthoritiesMapper());  // Apply custom role mapping
        return provider;
    }

    @Bean
    public LdapContextSource ldapContextSource() {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(ldapUrl);
        contextSource.setBase(ldapBase);
        contextSource.setUserDn(ldapUsername);
        contextSource.setPassword(ldapPassword);

        // Set the referral property
        contextSource.setReferral("ignore");  // or "follow" depending on your use case

        return contextSource;
    }


    @Bean
    public GrantedAuthoritiesMapper grantedAuthoritiesMapper() {
        return authorities -> {
            Collection<GrantedAuthority> mappedAuthorities = new ArrayList<>();

            for (GrantedAuthority authority : authorities) {
                logger.info("Processing authority: " + authority.getAuthority());

                // Normalize the authority to handle case insensitivity or formatting issues
                String normalizedAuthority = authority.getAuthority().toLowerCase();

                // Check if the authority contains "administrators"
                if (normalizedAuthority.contains("administrators")) {
                    logger.info("Mapping 'Administrators' to 'ROLE_ADMIN'");
                    mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                } else {
                    logger.info("Mapping '" + authority.getAuthority() + "' to default role");
                    mappedAuthorities.add(authority);  // Keep other roles
                }
            }

            logger.info("Mapped authorities: " + mappedAuthorities);
            return mappedAuthorities;
        };
    }
}
