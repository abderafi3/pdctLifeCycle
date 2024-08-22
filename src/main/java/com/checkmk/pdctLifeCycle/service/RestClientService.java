package com.checkmk.pdctLifeCycle.service;

import com.checkmk.pdctLifeCycle.config.CheckmkConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class RestClientService {
    private final CheckmkConfig checkmkConfig;
    private final RestTemplate restTemplate;

    @Autowired
    public RestClientService(CheckmkConfig checkmkConfig, RestTemplate restTemplate) {
        this.checkmkConfig = checkmkConfig;
        this.restTemplate = restTemplate;
    }

    private HttpEntity<String> createHttpEntity(String payload, String eTag) {
        String auth = checkmkConfig.getApiUsername() + ":" + checkmkConfig.getApiPassword();
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.US_ASCII));
        String authHeader = "Basic " + new String(encodedAuth);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", authHeader);
        if (eTag != null) {
            headers.set("If-Match", eTag);
        }

        return new HttpEntity<>(payload, headers);
    }

    public <T> ResponseEntity<T> sendGetRequest(String url, Class<T> responseType){
        HttpEntity<String> entity = createHttpEntity(null, null);
        return restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
    }

    public ResponseEntity<String> sendPostRequest(String url, String payload){
        HttpEntity<String> entity = createHttpEntity(payload, "*");
        return restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }

    public ResponseEntity<String> sendPutRequest(String url, String payload, String eTag){
        HttpEntity<String> entity = createHttpEntity(payload, eTag);
        return restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
    }

    public ResponseEntity<String> sendDeleteRequest(String url, String eTag){
        HttpEntity<String> entity = createHttpEntity(null, eTag);
        return restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
    }

    public String getEtag(String url){
        ResponseEntity<String> response = sendGetRequest(url, String.class);
        return response.getHeaders().getETag();
    }
}
