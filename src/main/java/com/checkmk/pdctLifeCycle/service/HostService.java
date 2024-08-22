package com.checkmk.pdctLifeCycle.service;

import com.checkmk.pdctLifeCycle.config.CheckmkConfig;
import com.checkmk.pdctLifeCycle.exception.HostServiceException;
import com.checkmk.pdctLifeCycle.model.Host;
import com.checkmk.pdctLifeCycle.model.HostLiveInfo;
import com.checkmk.pdctLifeCycle.model.HostWithLiveInfo;
import com.checkmk.pdctLifeCycle.repository.HostRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.ldap.userdetails.LdapUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HostService {

    private final HostRepository hostRepository;
    private final CheckmkConfig checkmkConfig;
    private final ObjectMapper objectMapper;
    private final RestClientService restClientService;
    private final HostLiveInfoService hostLiveInfoService;

    @Autowired
    public HostService(HostRepository hostRepository, CheckmkConfig checkmkConfig, ObjectMapper objectMapper, RestClientService restClientService, HostLiveInfoService hostLiveInfoService) {
        this.hostRepository = hostRepository;
        this.checkmkConfig = checkmkConfig;
        this.objectMapper = objectMapper;
        this.restClientService = restClientService;
        this.hostLiveInfoService = hostLiveInfoService;
    }

    public List<Host> getAllHosts() {
        return hostRepository.findAll();
    }

    public Host getHostById(String id) {
        return hostRepository.findById(id).orElse(null);
    }

    public List<Host> getHostsByUsername(String hostUserEmail) {
        return hostRepository.findByHostUserEmail(hostUserEmail);
    }

    public Host addHost(Host host) throws HostServiceException {
        host.setId(host.getHostName());

        // Check if hostname exists in the database
        if (hostRepository.existsByHostName(host.getHostName())) {
            throw new HostServiceException("Host name already exists in the database.");
        }

        // Check if hostname exists in Checkmk
        if (hostExistsInCheckmk(host.getHostName())) {
            throw new HostServiceException("Host name already exists in Checkmk.");
        }

        String apiUrl = checkmkConfig.getApiUrl() + "/api/1.0/domain-types/host_config/collections/all";

        try {
            // Create payload for adding the host to Checkmk
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("folder", "/");
            payload.put("host_name", host.getHostName());

            ObjectNode attributes = objectMapper.createObjectNode();
            attributes.put("ipaddress", host.getIpAddress());
            payload.set("attributes", attributes);

            // Send the request to add the host to Checkmk
            restClientService.sendPostRequest(apiUrl, payload.toString());

            // Trigger service discovery and accept the discovered services
            triggerServiceDiscoveryAndAccept(host.getHostName());

            // Activate changes in Checkmk
            this.checkmkActivateChanges();

            // Save the host to the database
            host.setCreationDate(LocalDate.now().toString());
            return hostRepository.save(host);

        } catch (HttpClientErrorException e) {
            throw new HostServiceException("Couldn't add a new host - API Error: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new HostServiceException("Couldn't add a new host. Please check if the Agent program is already installed.", e);
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

            ObjectNode payload = objectMapper.createObjectNode();
            ObjectNode attributes = objectMapper.createObjectNode();
            attributes.put("ipaddress", host.getIpAddress());
            payload.set("attributes", attributes);

            String eTag = getHostETag(hostName);

            restClientService.sendPutRequest(apiUrl, payload.toString(), eTag);
            this.checkmkActivateChanges();

            host.setHostName(hostName);
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
                String eTag = getHostETag(host.getHostName());
                String apiUrl = checkmkConfig.getApiUrl() + "/api/1.0/objects/host_config/" + host.getHostName();

                restClientService.sendDeleteRequest(apiUrl, eTag);
                this.checkmkActivateChanges();

                hostRepository.delete(host);
            } else {
                throw new HostServiceException("Host not found");
            }
        } catch (Exception e) {
            throw new HostServiceException("Couldn't delete the host", e);
        }
    }

    public List<HostWithLiveInfo> getHostsWithLiveInfo(Authentication authentication) {
        List<Host> hosts = getHostsByUserRole(authentication);
        return enrichHostsWithLiveInfo(hosts);
    }

    private List<Host> getHostsByUserRole(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            LdapUserDetails userDetails = (LdapUserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

            if (isAdmin) {
                return getAllHosts();  // Admin gets all hosts
            } else {
                return getHostsByUsername(username);  // Non-admin users get their own hosts
            }
        }
        return new ArrayList<>();
    }

    private List<HostWithLiveInfo> enrichHostsWithLiveInfo(List<Host> hosts) {
        return hosts.stream().map(host -> {
            HostWithLiveInfo hostWithLiveInfo = new HostWithLiveInfo();
            hostWithLiveInfo.setHost(host);
            try {
                HostLiveInfo liveInfo = hostLiveInfoService.convertToHostLiveInfo().stream()
                        .filter(info -> info.getHostName().equals(host.getHostName()))
                        .findFirst()
                        .orElse(new HostLiveInfo());
                hostWithLiveInfo.setLiveInfo(liveInfo);
            } catch (Exception e) {
                e.printStackTrace();
                hostWithLiveInfo.setLiveInfo(new HostLiveInfo());
            }
            return hostWithLiveInfo;
        }).collect(Collectors.toList());
    }

    public void checkmkActivateChanges() {
        String apiUrl = checkmkConfig.getApiUrl() + "/api/1.0/domain-types/activation_run/actions/activate-changes/invoke";

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("redirect", false);
        payload.putArray("sites").add(checkmkConfig.getCheckmkSite());
        payload.put("force_foreign_changes", true);

        restClientService.sendPostRequest(apiUrl, payload.toString());
    }

    public String getHostETag(String hostName) {
        String url = checkmkConfig.getApiUrl() + "/api/1.0/objects/host_config/" + hostName;
        return restClientService.getEtag(url);
    }





    public void triggerServiceDiscoveryAndAccept(String hostName) throws HostServiceException {
        String discoveryApiUrl = checkmkConfig.getApiUrl() + "/api/1.0/domain-types/service_discovery_run/actions/start/invoke";
        String waitDiscoveryCompletionUrl = checkmkConfig.getApiUrl() + "/api/1.0/objects/service_discovery_run/" +hostName + "/actions/wait-for-completion/invoke";

        try {
            // Step 1: Trigger "refresh" to rescan services
            ObjectNode refreshPayload = objectMapper.createObjectNode();
            refreshPayload.put("host_name", hostName);
            refreshPayload.put("mode", "refresh");  // Rescan services

            restClientService.sendPostRequest(discoveryApiUrl, refreshPayload.toString());

            // Step 2: Handle redirect to "Wait for service discovery completion"
            boolean isCompleted = waitForServiceDiscoveryCompletion(waitDiscoveryCompletionUrl);
            if (!isCompleted) {
                throw new HostServiceException("Service discovery did not complete within the expected time.");
            }

            // Step 3: Trigger "fix_all" to accept all services into monitored phase
            ObjectNode fixAllPayload = objectMapper.createObjectNode();
            fixAllPayload.put("host_name", hostName);
            fixAllPayload.put("mode", "fix_all");  // Accept all services into monitored phase

            restClientService.sendPostRequest(discoveryApiUrl, fixAllPayload.toString());

            System.out.println("Service discovery completed and all services accepted for host: " + hostName);

        } catch (HttpClientErrorException e) {
            throw new HostServiceException("Couldn't trigger service discovery for host: " + hostName + " - API Error: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new HostServiceException("An error occurred while moving services to monitored phase", e);
        }
    }

    private boolean waitForServiceDiscoveryCompletion(String waitUrl) throws HostServiceException {
        // Handle the polling mechanism here to wait for service discovery to complete.
        // This can involve sending GET requests periodically to check the status and
        // returning true once the discovery is complete.

        try {
            // Simulate polling - send a GET request to the wait endpoint and check the response
            ResponseEntity<String> response = restClientService.sendGetRequest(waitUrl, String.class);
            // Process the response and determine if service discovery is completed
            // This logic depends on the structure of the response from the Checkmk API

            // Example success check (you would need to adapt this based on the actual API)
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            throw new HostServiceException("Failed to wait for service discovery completion.", e);
        }
    }



//    public void triggerServiceDiscovery(String hostName) throws HostServiceException {
//        String apiUrl = checkmkConfig.getApiUrl() + "/api/1.0/domain-types/service_discovery_run/actions/start/invoke";
//        try {
//            // Prepare payload for service discovery
//            ObjectNode payload = objectMapper.createObjectNode();
//            payload.put("host_name", hostName);
//            payload.put("mode", "new"); // You can choose between "new", "refresh", "fixall"
//
//            // Send the request to trigger service discovery
//            restClientService.sendPostRequest(apiUrl, payload.toString());
//
//
//
//        } catch (Exception e) {
//            throw new HostServiceException("Couldn't trigger service discovery for host: " + hostName, e);
//        }
//    }



    public boolean hostExistsInDatabase(String hostName) {
        return hostRepository.existsByHostName(hostName);
    }

    public boolean hostExistsInCheckmk(String hostName) {
        String apiUrl = checkmkConfig.getApiUrl() + "/api/1.0/objects/host_config/" + hostName;
        try {
            return restClientService.sendGetRequest(apiUrl, String.class).getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}
