package com.checkmk.pdctLifeCycle.service;

import com.checkmk.pdctLifeCycle.config.CheckmkConfig;
import com.checkmk.pdctLifeCycle.exception.HostServiceException;
import com.checkmk.pdctLifeCycle.model.*;
import com.checkmk.pdctLifeCycle.repository.HostRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.reactive.function.client.WebClient;

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
    private final LdapUserService ldapUserService;

   private final WebClient webClient;

    @Autowired
    public HostService(HostRepository hostRepository, CheckmkConfig checkmkConfig,
                       ObjectMapper objectMapper, RestClientService restClientService,
                       HostLiveInfoService hostLiveInfoService, WebClient.Builder webClientBuilder,
                       LdapUserService ldapUserService) {
        this.hostRepository = hostRepository;
        this.checkmkConfig = checkmkConfig;
        this.objectMapper = objectMapper;
        this.restClientService = restClientService;
        this.ldapUserService = ldapUserService;
        this.hostLiveInfoService = hostLiveInfoService;
        this.webClient = webClientBuilder.baseUrl(checkmkConfig.getApiUrl()).build();
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

    public Host addHost(Host host) throws HostServiceException {
        if (hostExistsInDatabase(host.getHostName()) || hostExistsInCheckmk(host.getHostName())) {
            throw new HostServiceException("Host name already exists.");
        }

        host.setId(host.getHostName());
        try {
            ObjectNode payload = buildCheckmkPayload(host);
            String apiUrl = checkmkConfig.getApiUrl() + "/api/1.0/domain-types/host_config/collections/all";

            restClientService.sendPostRequest(apiUrl, payload.toString());
            checkmkActivateChanges();

            host.setCreationDate(LocalDate.now().toString());
            return hostRepository.save(host);
        } catch (Exception e) {
            throw new HostServiceException("Couldn't add the new host", e);
        }
    }

    // Update an existing host
    public Host updateHost(Host host) throws HostServiceException {
        Host existingHost = getHostById(host.getId());
        if (existingHost == null) throw new HostServiceException("Host not found");

        String hostName = existingHost.getHostName();
        String apiUrl = checkmkConfig.getApiUrl() + "/api/1.0/objects/host_config/" + hostName;

        try {
            ObjectNode payload = buildUpdatePayload(host);
            String eTag = getHostETag(hostName);

            restClientService.sendPutRequest(apiUrl, payload.toString(), eTag);
            checkmkActivateChanges();

            host.setHostName(hostName);
            host.setCreationDate(existingHost.getCreationDate());
            return hostRepository.save(host);
        } catch (Exception e) {
            throw new HostServiceException("Couldn't update the host", e);
        }
    }

    // Delete a host from both the database and Checkmk
    public void deleteHost(String id) throws HostServiceException {
        Host host = getHostById(id);
        if (host == null) throw new HostServiceException("Host not found");

        String apiUrl = checkmkConfig.getApiUrl() + "/api/1.0/objects/host_config/" + host.getHostName();
        try {
            String eTag = getHostETag(host.getHostName());
            restClientService.sendDeleteRequest(apiUrl, eTag);
            checkmkActivateChanges();
            hostRepository.delete(host);
        } catch (Exception e) {
            throw new HostServiceException("Couldn't delete the host", e);
        }
    }

    public List<HostWithLiveInfo> getHostsWithLiveInfo(Authentication authentication) {
        List<Host> hosts = getHostsByUserRole(authentication);
        return enrichHostsWithLiveInfo(hosts);
    }

    public List<Host> getHostsByUserRole(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ArrayList<>();
        }

        LdapUser ldapUser = (LdapUser) authentication.getPrincipal();

        if (isAdmin(authentication)) {
            return getAllHosts();  // Admin gets all hosts
        } else if (isDepartmentHead(authentication)) {
            return getHostsForDepartment(ldapUser.getDepartment());
        } else if (isTeamLeader(authentication)) {
            return getHostsForTeam(ldapUser.getTeam());
        } else {
            return getHostsByUsername(ldapUser.getUsername());  // Regular user gets only their own hosts
        }
    }

    // Helper method to enrich hosts with live info
    private List<HostWithLiveInfo> enrichHostsWithLiveInfo(List<Host> hosts) {
        return hosts.stream()
                .map(host -> {
                    HostWithLiveInfo hostWithLiveInfo = new HostWithLiveInfo();
                    hostWithLiveInfo.setHost(host);
                    hostWithLiveInfo.setLiveInfo(fetchHostLiveInfo(host));
                    return hostWithLiveInfo;
                })
                .collect(Collectors.toList());
    }

    private HostLiveInfo fetchHostLiveInfo(Host host) {
        try {
            return hostLiveInfoService.convertToHostLiveInfo()
                    .stream()
                    .filter(info -> info.getHostName().equals(host.getHostName()))
                    .findFirst()
                    .orElse(new HostLiveInfo());
        } catch (Exception e) {
            return new HostLiveInfo();
        }
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

    // Helper method to build Checkmk payload for adding a host
    private ObjectNode buildCheckmkPayload(Host host) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("folder", "/");
        payload.put("host_name", host.getHostName());
        ObjectNode attributes = objectMapper.createObjectNode();
        attributes.put("ipaddress", host.getIpAddress());
        payload.set("attributes", attributes);
        return payload;
    }

    // Helper method to build payload for updating a host
    private ObjectNode buildUpdatePayload(Host host) {
        ObjectNode payload = objectMapper.createObjectNode();
        ObjectNode attributes = objectMapper.createObjectNode();
        attributes.put("ipaddress", host.getIpAddress());
        payload.set("attributes", attributes);
        return payload;
    }

    public void triggerServiceDiscoveryAndMonitor(String hostName) throws HostServiceException {
        String discoveryApiUrl = checkmkConfig.getApiUrl() + "/api/1.0/domain-types/service_discovery_run/actions/start/invoke";
        String waitDiscoveryCompletionUrl = checkmkConfig.getApiUrl() + "/api/1.0/objects/service_discovery_run/" + hostName + "/actions/wait-for-completion/invoke";

        try {
            ObjectNode refreshPayload = objectMapper.createObjectNode();
            refreshPayload.put("host_name", hostName);
            refreshPayload.put("mode", "refresh");
            restClientService.sendPostRequest(discoveryApiUrl, refreshPayload.toString());

            // Poll for service discovery completion
            if (!waitForServiceDiscoveryCompletion(waitDiscoveryCompletionUrl)) {
                throw new HostServiceException("Service discovery did not complete within the expected time.");
            }

            // Trigger "fix_all" to accept all services into monitored phase
            ObjectNode fixAllPayload = objectMapper.createObjectNode();
            fixAllPayload.put("host_name", hostName);
            fixAllPayload.put("mode", "fix_all");
            restClientService.sendPostRequest(discoveryApiUrl, fixAllPayload.toString());
            checkmkActivateChanges();
        } catch (ResourceAccessException e) {
            if (e.getCause() instanceof java.net.ProtocolException) {
                throw new HostServiceException("Too many redirects while trying to monitor services for host: " + hostName, e);
            } else {
                throw new HostServiceException("Failed to connect to the monitoring service for host: " + hostName, e);
            }
        } catch (Exception e) {
            throw new HostServiceException("An error occurred while moving services to monitored phase", e);
        }
    }

    private boolean waitForServiceDiscoveryCompletion(String waitUrl) throws InterruptedException, HostServiceException {
        int retries = 10;
        int delay = 3000; // 3 seconds between retries

        for (int i = 0; i < retries; i++) {
            try {
                ResponseEntity<String> response = restClientService.sendGetRequest(waitUrl, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    return true;
                }
                Thread.sleep(delay);

            } catch (Exception e) {

            }
        }

        return false;
    }

    // Check if the current user is an admin
    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean isDepartmentHead(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_DEPARTMENTHEAD"));
    }

    private boolean isTeamLeader(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_TEAMLEADER"));
    }

    private List<Host> getHostsForDepartment(String department) {
        return getAllHosts().stream()
                .filter(host -> {
                    LdapUser hostUser = ldapUserService.findUserByEmail(host.getHostUserEmail());
                    return hostUser != null && department.equals(hostUser.getDepartment());
                })
                .collect(Collectors.toList());
    }

    private List<Host> getHostsForTeam(String team) {
        return getAllHosts().stream()
                .filter(host -> {
                    LdapUser hostUser = ldapUserService.findUserByEmail(host.getHostUserEmail());
                    return hostUser != null && team.equals(hostUser.getTeam());
                })
                .collect(Collectors.toList());
    }
}
