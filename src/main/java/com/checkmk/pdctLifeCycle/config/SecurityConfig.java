package com.checkmk.pdctLifeCycle.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    @Value("${spring.ldap.domain}")
    private String ldapDomain;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/error", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/hosts/add", "/hosts/edit/**", "/hosts/delete/**", "/hosts/import").hasRole("ADMIN")
                        .requestMatchers("/hosts").authenticated()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login").permitAll()
                        .defaultSuccessUrl("/hosts", true))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout").permitAll())
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean
    public ActiveDirectoryLdapAuthenticationProvider activeDirectoryLdapAuthenticationProvider() {
        ActiveDirectoryLdapAuthenticationProvider provider =
                new ActiveDirectoryLdapAuthenticationProvider(ldapDomain, ldapUrl);
        provider.setConvertSubErrorCodesToExceptions(true);
        provider.setUseAuthenticationRequestCredentials(true);
        provider.setAuthoritiesMapper(grantedAuthoritiesMapper());
        return provider;
    }


    @Bean
    public GrantedAuthoritiesMapper grantedAuthoritiesMapper() {
        return authorities -> {
            Collection<GrantedAuthority> mappedAuthorities = new ArrayList<>();
            for (GrantedAuthority authority : authorities) {
                String normalizedAuthority = authority.getAuthority().toLowerCase();
                if (normalizedAuthority.contains("administrators")) {
                    mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                } else {
                    mappedAuthorities.add(authority);
                }
            }
            return mappedAuthorities;
        };
    }
}
