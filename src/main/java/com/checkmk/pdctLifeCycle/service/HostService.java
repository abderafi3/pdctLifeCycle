package com.checkmk.pdctLifeCycle.service;

import com.checkmk.pdctLifeCycle.config.CheckmkConfig;
import com.checkmk.pdctLifeCycle.exception.HostServiceException;
import com.checkmk.pdctLifeCycle.model.Host;
import com.checkmk.pdctLifeCycle.model.HostUser;
import com.checkmk.pdctLifeCycle.repository.HostRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class HostService {

    private static final Logger logger = LoggerFactory.getLogger(HostService.class);

    private final HostRepository hostRepository;
    private final CheckmkConfig checkmkConfig;
    private final ObjectMapper objectMapper;
    private final RestClientService restClientService;

    @Autowired
    public HostService(HostRepository hostRepository, CheckmkConfig checkmkConfig, ObjectMapper objectMapper, RestClientService restClientService) {
        this.hostRepository = hostRepository;
        this.checkmkConfig = checkmkConfig;
        this.objectMapper = objectMapper;
        this.restClientService = restClientService;
    }

    public List<Host> getAllHosts() {
        return hostRepository.findAll();
    }

    public Host getHostById(String id) {
        return hostRepository.findById(id).orElse(null);
    }

    public List<Host> getHostsByUser(HostUser user) {
        return hostRepository.findByHostUser(user);
    }

    public Host addHost(Host host) throws HostServiceException {
        host.setId(host.getHostName());

        String apiUrl = checkmkConfig.getApiUrl() + "/api/1.0/domain-types/host_config/collections/all";

        try {
            // Payload
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("folder", "/");
            payload.put("host_name", host.getHostName());

            ObjectNode attribute = objectMapper.createObjectNode();
            attribute.put("ipaddress", host.getIpAddress());
            payload.set("attributes", attribute);

            // Save in Checkmk
            restClientService.sendPostRequest(apiUrl, payload.toString());
            this.checkmkActivateChanges();

            // Save in Database
            host.setCreationDate(LocalDate.now().toString());
            return hostRepository.save(host);
        } catch (Exception e) {
            logger.error("Couldn't add a new host", e);
            throw new HostServiceException("Couldn't add a new host", e);
        }
    }

    public Host updateHost(Host host) throws HostServiceException {
        try {
            Host existingHost = getHostById(host.getId());
            if (existingHost == null) {
                throw new HostServiceException("Host not found");
            }

            String hostName = existingHost.getHostName();

            String apiUrl = checkmkConfig.getApiUrl() + "/api/1.0/objects/host_config/" + hostName;

            // Payload
            ObjectNode payload = objectMapper.createObjectNode();
            ObjectNode attributes = objectMapper.createObjectNode();
            attributes.put("ipaddress", host.getIpAddress());
            payload.set("attributes", attributes);

            // Fetch ETag
            String eTag = getHostETag(hostName);

            // Update host in Checkmk
            restClientService.sendPutRequest(apiUrl, payload.toString(), eTag);
            this.checkmkActivateChanges();

            // Save host in the database
            host.setHostName(hostName);// Ensure the host name is set
            host.setCreationDate(existingHost.getCreationDate());
            return hostRepository.save(host);
        } catch (Exception e) {
            throw new HostServiceException("Couldn't update the host", e);
        }
    }

    public void deleteHost(String id) throws HostServiceException {
        try {
            Host host = getHostById(id);
            if (host != null) {
                // Fetch ETag
                String eTag = getHostETag(host.getHostName());
                String apiUrl = checkmkConfig.getApiUrl() + "/api/1.0/objects/host_config/" + host.getHostName();
                restClientService.sendDeleteRequest(apiUrl, eTag);
                this.checkmkActivateChanges();
                hostRepository.delete(host);
            } else {
                throw new HostServiceException("Host not found");
            }
        } catch (Exception e) {
            logger.error("Couldn't delete the host", e);
            throw new HostServiceException("Couldn't delete the host", e);
        }
    }

    public void checkmkActivateChanges() {
        String apiUrl = checkmkConfig.getApiUrl() + "/api/1.0/domain-types/activation_run/actions/activate-changes/invoke";

        // Payload
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("redirect", false);
        payload.putArray("sites").add(checkmkConfig.getCheckmkSite());
        payload.put("force_foreign_changes", true);

        // Send request
        restClientService.sendPostRequest(apiUrl, payload.toString());
    }

    public String getHostETag(String hostName) {
        String url = checkmkConfig.getApiUrl() + "/api/1.0/objects/host_config/" + hostName;
        return restClientService.getEtag(url);
    }

    private boolean hostExistsInCheckmk(String hostName) {
        try {
            String url = checkmkConfig.getApiUrl() + "/api/1.0/objects/host_config/" + hostName;
            ResponseEntity<String> response = restClientService.sendGetRequest(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}
