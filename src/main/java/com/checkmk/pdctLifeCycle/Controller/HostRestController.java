package com.checkmk.pdctLifeCycle.Controller;

import com.checkmk.pdctLifeCycle.exception.HostServiceException;
import com.checkmk.pdctLifeCycle.model.Host;
import com.checkmk.pdctLifeCycle.model.HostLiveInfo;
import com.checkmk.pdctLifeCycle.model.LdapUser;
import com.checkmk.pdctLifeCycle.service.HostImportService;
import com.checkmk.pdctLifeCycle.service.HostLiveInfoService;
import com.checkmk.pdctLifeCycle.service.HostService;
import com.checkmk.pdctLifeCycle.service.LdapUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("api/hosts")
public class HostRestController {

    @Autowired
    public HostService hostService;

    @Autowired
    public HostImportService hostImportService;

    @Autowired
    public HostLiveInfoService hostLiveInfoService;

    @Autowired
    public LdapUserService ldapUserService;

    // Allow any authenticated user to view all hosts
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<Host> getAllDatabaseHosts() {
        return hostService.getAllHosts();
    }

    //for testing purposes

    @GetMapping("/users")
    public List<LdapUser> getAllLdapUsers() {
        return ldapUserService.getAllUsers();
    }

    // Allow any authenticated user to view Checkmk hosts
    @GetMapping("import")
    @PreAuthorize("isAuthenticated()")
    public List<Host> getAllCheckmkHosts() {
        return hostImportService.getCheckMkHosts();
    }

    // Allow any authenticated user to view host live information
    @GetMapping("info")
    @PreAuthorize("isAuthenticated()")
    public List<HostLiveInfo> getAllHostInfo() throws Exception {
        return hostLiveInfoService.convertToHostLiveInfo();
    }

    // Allow only users with the ADMIN role to add hosts
    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public Host addHost(@RequestBody Host host) throws HostServiceException {
        return hostService.addHost(host);
    }

    // Allow only users with the ADMIN role to update hosts
    @PutMapping("{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Host updateHost(@PathVariable String id, @RequestBody Host host) throws HostServiceException {
        host.setId(id);
        return hostService.updateHost(host);
    }

    // Allow only users with the ADMIN role to delete hosts
    @DeleteMapping("{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void deleteHost(@PathVariable String id) throws HostServiceException {
        Host host = hostService.getHostById(id);
        if (host == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Host not found");
        }
        hostService.deleteHost(id);
    }

    // Allow only users with the ADMIN role to save imported hosts
    @PostMapping("import")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void saveImportedHosts(@RequestBody List<String> selectedHostIds) {
        hostImportService.saveSelectedHosts(selectedHostIds);
    }
}
