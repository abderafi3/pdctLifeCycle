package com.checkmk.pdctLifeCycle.Service;

import com.checkmk.pdctLifeCycle.config.CheckmkConfig;
import com.checkmk.pdctLifeCycle.config.RestTemplateConfig;
import com.checkmk.pdctLifeCycle.exception.HostServiceException;
import com.checkmk.pdctLifeCycle.model.Host;
import com.checkmk.pdctLifeCycle.model.HostLiveInfo;
import com.checkmk.pdctLifeCycle.model.HostWithLiveInfo;
import com.checkmk.pdctLifeCycle.model.LdapUser;
import com.checkmk.pdctLifeCycle.repository.HostRepository;
import com.checkmk.pdctLifeCycle.service.HostLiveInfoService;
import com.checkmk.pdctLifeCycle.service.HostService;
import com.checkmk.pdctLifeCycle.service.LdapUserService;
import com.checkmk.pdctLifeCycle.service.RestClientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Import(RestTemplateConfig.class) // Import necessary configuration
class HostServiceTest {

    @Mock
    private HostRepository hostRepository;
    @Mock
    private CheckmkConfig checkmkConfig;

    private ObjectMapper objectMapper;

    @Mock
    private RestClientService restClientService;
    @Mock
    private HostLiveInfoService hostLiveInfoService;

    @Mock
    private LdapUserService ldapUserService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private HostService hostService;

    @Mock
    private Host mockHost;

    @Mock
    private LdapUser mockLdapUser;

    @Mock
    private  WebClient.Builder webClient;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Initialize mockHost and other necessary objects
        mockHost = new Host();
        mockHost.setId("test-host");
        mockHost.setHostName("test-host");
        mockHost.setIpAddress("192.168.0.1");
        mockHost.setHostUserEmail("user@example.com");

        // Initialize ObjectMapper
        objectMapper = new ObjectMapper(); // Initialize the real ObjectMapper
        objectMapper = new ObjectMapper(); // Initialize ObjectMapper
        hostService = new HostService(hostRepository, checkmkConfig, objectMapper, restClientService, hostLiveInfoService, webClient, ldapUserService);

        // Setup for mockLdapUser and other mocks
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        mockLdapUser = new LdapUser("firstName", "lastName", "user@example.com", "IT", "Dev", authorities);
    }

    @Test
    void testGetAllHosts() {
        when(hostRepository.findAll()).thenReturn(List.of(mockHost));

        List<Host> hosts = hostService.getAllHosts();

        assertNotNull(hosts);
        assertEquals(1, hosts.size());
        assertEquals("test-host", hosts.get(0).getHostName());
        verify(hostRepository, times(1)).findAll();
    }

    @Test
    void testGetHostById() {
        when(hostRepository.findById(anyString())).thenReturn(Optional.of(mockHost));

        Host host = hostService.getHostById("test-host");

        assertNotNull(host);
        assertEquals("test-host", host.getHostName());
        verify(hostRepository, times(1)).findById("test-host");
    }

    @Test
    void testGetHostsByUsername() {
        when(hostRepository.findByHostUserEmail(anyString())).thenReturn(List.of(mockHost));

        List<Host> hosts = hostService.getHostsByUsername("user@example.com");

        assertNotNull(hosts);
        assertEquals(1, hosts.size());
        assertEquals("test-host", hosts.get(0).getHostName());
        verify(hostRepository, times(1)).findByHostUserEmail("user@example.com");
    }

    @Test
    void testHostExistsInDatabase() {
        when(hostRepository.existsByHostName(anyString())).thenReturn(true);

        boolean exists = hostService.hostExistsInDatabase("test-host");

        assertTrue(exists);
        verify(hostRepository, times(1)).existsByHostName("test-host");
    }

    @Test
    void testHostExistsInCheckmk() {
        // Mock the base URL if it's being fetched from a configuration service like CheckmkConfig
        when(checkmkConfig.getApiUrl()).thenReturn("http://mock-checkmk-api.com");

        // Mock the restClientService behavior
        when(restClientService.sendGetRequest(eq("http://mock-checkmk-api.com/api/1.0/objects/host_config/test-host"), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // Call the method under test
        boolean exists = hostService.hostExistsInCheckmk("test-host");

        // Verify the result
        assertTrue(exists);

        // Verify that sendGetRequest was called with the correct URL
        verify(restClientService, times(1)).sendGetRequest(eq("http://mock-checkmk-api.com/api/1.0/objects/host_config/test-host"), eq(String.class));
    }




    @Test
    void testAddHost() throws Exception {
        // Simuliert, dass der Host nicht in der Datenbank existiert
        when(hostRepository.existsByHostName(anyString())).thenReturn(false);

        // Simuliert, dass der Host nicht in Checkmk existiert
        when(restClientService.sendGetRequest(anyString(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));

        // Speichert den Host in der Datenbank und gibt das Mock-Host-Objekt zurück
        when(hostRepository.save(any(Host.class))).thenReturn(mockHost);

        Host host = hostService.addHost(mockHost);

        // Verifiziert, dass der zurückgegebene Host nicht null ist
        assertNotNull(host, "Der zurückgegebene Host sollte nicht null sein");

        // Verifiziert, dass der Hostname dem erwarteten Wert entspricht
        assertEquals("test-host", host.getHostName(), "Der Hostname sollte dem erwarteten Wert entsprechen");

        // Verifiziert, dass sendPostRequest zweimal aufgerufen wurde:
        // einmal zum Hinzufügen des Hosts und einmal zum Aktivieren der Änderungen
        verify(restClientService, times(2)).sendPostRequest(anyString(), anyString());

        // Verifiziert, dass der Host genau einmal in der Repository gespeichert wurde
        verify(hostRepository, times(1)).save(mockHost);
    }




    @Test
    void testAddHostThrowsExceptionWhenHostExists() {
        when(hostRepository.existsByHostName(anyString())).thenReturn(true);

        assertThrows(HostServiceException.class, () -> hostService.addHost(mockHost));
    }

    @Test
    void testUpdateHost() throws Exception {
        when(hostRepository.findById(anyString())).thenReturn(Optional.of(mockHost));
        when(objectMapper.createObjectNode()).thenReturn(new ObjectMapper().createObjectNode());
        when(restClientService.getEtag(anyString())).thenReturn("etag-value");
        when(hostRepository.save(any(Host.class))).thenReturn(mockHost);

        Host updatedHost = hostService.updateHost(mockHost);

        assertNotNull(updatedHost);
        assertEquals("test-host", updatedHost.getHostName());
        verify(restClientService, times(1)).sendPutRequest(anyString(), anyString(), anyString());
        verify(hostRepository, times(1)).save(mockHost);
    }

    @Test
    void testUpdateHostThrowsExceptionWhenHostNotFound() {
        when(hostRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThrows(HostServiceException.class, () -> hostService.updateHost(mockHost));
    }

    @Test
    void testDeleteHost() throws Exception {
        when(hostRepository.findById(anyString())).thenReturn(Optional.of(mockHost));
        when(restClientService.getEtag(anyString())).thenReturn("etag-value");

        hostService.deleteHost("test-host");

        verify(restClientService, times(1)).sendDeleteRequest(anyString(), anyString());
        verify(hostRepository, times(1)).delete(mockHost);
    }

    @Test
    void testDeleteHostThrowsExceptionWhenHostNotFound() {
        when(hostRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThrows(HostServiceException.class, () -> hostService.deleteHost("test-host"));
    }

    @Test
    void testGetHostsWithLiveInfo() throws Exception {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(mockLdapUser);
        when(hostRepository.findByHostUserEmail(anyString())).thenReturn(List.of(mockHost));
        when(hostLiveInfoService.convertToHostLiveInfo()).thenReturn(List.of(new HostLiveInfo()));

        List<HostWithLiveInfo> hostsWithLiveInfo = hostService.getHostsWithLiveInfo(authentication);

        assertNotNull(hostsWithLiveInfo);
        assertEquals(1, hostsWithLiveInfo.size());
        assertEquals("test-host", hostsWithLiveInfo.get(0).getHost().getHostName());
        verify(hostLiveInfoService, times(1)).convertToHostLiveInfo();
    }

    @Test
    void testCheckmkActivateChanges() {
        when(objectMapper.createObjectNode()).thenReturn(new ObjectMapper().createObjectNode());

        hostService.checkmkActivateChanges();

        verify(restClientService, times(1)).sendPostRequest(anyString(), anyString());
    }

    @Test
    void testTriggerServiceDiscoveryAndMonitor() throws HostServiceException {
        when(objectMapper.createObjectNode()).thenReturn(new ObjectMapper().createObjectNode());
        when(restClientService.sendGetRequest(anyString(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        assertDoesNotThrow(() -> hostService.triggerServiceDiscoveryAndMonitor("test-host"));

        verify(restClientService, times(2)).sendPostRequest(anyString(), anyString());
    }

    @Test
    void testTriggerServiceDiscoveryAndMonitorThrowsException() {
        when(objectMapper.createObjectNode()).thenReturn(new ObjectMapper().createObjectNode());
        when(restClientService.sendGetRequest(anyString(), eq(String.class)))
                .thenThrow(ResourceAccessException.class);

        assertThrows(HostServiceException.class, () -> hostService.triggerServiceDiscoveryAndMonitor("test-host"));
    }
}
