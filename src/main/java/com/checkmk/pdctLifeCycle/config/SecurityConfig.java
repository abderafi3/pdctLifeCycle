package com.checkmk.pdctLifeCycle.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/register", "/error", "/css/**", "/js/**").permitAll() // Allow access to login, error, and resources
                        .requestMatchers("/hosts/add", "/hosts/edit/**", "/hosts/delete/**", "/hosts/import").hasRole("ADMIN")  // Admin only routes
                        .requestMatchers("/hosts").authenticated()  // All authenticated users can access /hosts
                        .anyRequest().authenticated() // All other URLs require authentication
                )
                .formLogin(form -> form
                        .loginPage("/login").permitAll() // Ensure login page is accessible
                        .defaultSuccessUrl("/hosts", true) // Redirect after login
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout").permitAll() // Redirect to login after logout
                )
                .csrf(csrf -> csrf.disable()); // Disable CSRF for now

        return http.build();
    }
}
