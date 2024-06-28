package com.checkmk.pdctLifeCycle.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;

@Configuration
public class ThymeleafConfig {

    @Bean
    public SpringSecurityDialect getSpringSecurityDialect(){
        return new SpringSecurityDialect();
    }
}
