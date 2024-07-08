package com.checkmk.pdctLifeCycle.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CheckmkConfig {

    @Value("${checkmk.api.url}")
    private String apiUrl;

    @Value("${checkmk.api.username}")
    private String apiUsername;

    @Value("${checkmk.api.password}")
    private String apiPassword;

    @Value("${checkmk.api.site}")
    private String checkmkSite;

    //Getter

    public String getApiUrl() {
        return apiUrl;
    }

    public String getApiUsername() {
        return apiUsername;
    }

    public String getApiPassword() {
        return apiPassword;
    }

    public String getCheckmkSite() {
        return checkmkSite;
    }
}
