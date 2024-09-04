package com.checkmk.pdctLifeCycle.Controller;

import com.checkmk.pdctLifeCycle.exception.HostServiceException;
import com.checkmk.pdctLifeCycle.model.Host;
import com.checkmk.pdctLifeCycle.model.HostWithLiveInfo;
import com.checkmk.pdctLifeCycle.model.LdapUser;
import com.checkmk.pdctLifeCycle.model.ServiceInfo;
import com.checkmk.pdctLifeCycle.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/hosts")
public class HostWebController {

    private final HostService hostService;
    private final HostImportService hostImportService;
    private final LdapUserService ldapUserService;
    private final HostLiveInfoService hostLiveInfoService;
    private final SshService sshService;

    @Autowired
    public HostWebController(HostService hostService, HostImportService hostImportService,
                             LdapUserService ldapUserService,HostLiveInfoService hostLiveInfoService, SshService sshService) {
        this.hostService = hostService;
        this.hostImportService = hostImportService;
        this.ldapUserService = ldapUserService;
        this.hostLiveInfoService = hostLiveInfoService;
        this.sshService = sshService;
    }

    @GetMapping
    public String getHostsPage(Model model, Authentication authentication) {
        List<LdapUser> users = ldapUserService.getAllUsers();
        LdapUser currentUser = (LdapUser) authentication.getPrincipal();
        List<String> roles = currentUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        List<Host> hosts = hostService.getHostsByUserRole(authentication);
        model.addAttribute("hosts", hosts);
        model.addAttribute("userRoles", roles);
        model.addAttribute("pageTitle", "Hosts with Live Info");
        return "host/list";
    }

    @GetMapping("/live-data")
    @ResponseBody
    public List<HostWithLiveInfo> getLiveHostData(Authentication authentication) {
        return hostService.getHostsWithLiveInfo(authentication);
    }

    @GetMapping("/add")
    public String showAddHostForm(Model model, Authentication authentication) {
        LdapUser currentUser = (LdapUser) authentication.getPrincipal();
        List<LdapUser> users = getUsersBasedOnRole(currentUser);
        model.addAttribute("host", new Host());
        model.addAttribute("users", users);
        model.addAttribute("pageTitle", "Add Host");
        return "host/add";
    }


    @PostMapping("/add")
    public String addHost(@RequestParam("user") String userEmail, @ModelAttribute Host host) throws HostServiceException {
        LdapUser assignedUser = ldapUserService.findUserByEmail(userEmail);
        if (assignedUser != null) {
            host.setHostUser(assignedUser.getFirstName() + ' ' + assignedUser.getLastName());
            host.setHostUserEmail(assignedUser.getEmail());
        }
        hostService.addHost(host);
        return "redirect:/hosts";
    }


    @GetMapping("/edit/{id}")
    public String showEditHostForm(@PathVariable String id, Model model, Authentication authentication) {
        Host host = hostService.getHostById(id);
        LdapUser currentUser = (LdapUser) authentication.getPrincipal();
        List<LdapUser> users = getUsersBasedOnRole(currentUser);

        model.addAttribute("host", host);
        model.addAttribute("users", users);
        model.addAttribute("pageTitle", "Edit Host");
        return "host/edit";
    }

    @PostMapping("/edit/{id}")
    public String updateHost(@PathVariable String id, @RequestParam("user") String userEmail, @ModelAttribute Host host) throws HostServiceException {
        host.setId(id);
        LdapUser assignedUser = ldapUserService.findUserByEmail(userEmail);
        if (assignedUser != null) {
            host.setHostUser(assignedUser.getFirstName() + ' ' + assignedUser.getLastName());
            host.setHostUserEmail(assignedUser.getEmail());
        }
        hostService.updateHost(host);
        return "redirect:/hosts";
    }

    @GetMapping("/delete/{id}")
    public String deleteHost(@PathVariable String id) throws HostServiceException {
        hostService.deleteHost(id);
        return "redirect:/hosts";
    }

    @GetMapping("/import")
    public String importHosts(Model model) {
        List<Host> checkmkHosts = hostImportService.getCheckMkHosts();
        List<Host> dbHosts = hostService.getAllHosts();
        model.addAttribute("checkmkHosts", checkmkHosts);
        model.addAttribute("dbHosts", dbHosts);
        model.addAttribute("pageTitle", "Import Hosts");
        return "host/import";
    }

    @PostMapping("/import")
    public String saveImportedHosts(@RequestParam("selectedHostIds") List<String> selectedHostIds) {
        hostImportService.saveSelectedHosts(selectedHostIds);
        return "redirect:/hosts/import";
    }

    @GetMapping("/validate-hostname")
    @ResponseBody
    public Map<String, Boolean> validateHostName(@RequestParam String hostName) {
        boolean existsInDatabase = hostService.hostExistsInDatabase(hostName);
        boolean existsInCheckmk = hostService.hostExistsInCheckmk(hostName);

        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", existsInDatabase || existsInCheckmk);
        return response;
    }


    @PostMapping("/monitor/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> monitorHost(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Host host = hostService.getHostById(id);
            if (host == null) {
                throw new HostServiceException("Host not found");
            }
            hostService.triggerServiceDiscoveryAndMonitor(host.getHostName());

            response.put("success", true);
            response.put("message", "Service discovery and monitoring initiated successfully!");
            return ResponseEntity.ok(response);
        } catch (HostServiceException e) {

            response.put("success", false);
            response.put("message", "Couldn't monitor the host: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @GetMapping("/service-ok/{hostName}")
    public String getServicesOk(@PathVariable String hostName, Model model) throws Exception {
        List<ServiceInfo> servicesOk = hostLiveInfoService.getServiceOKs(hostName);
        model.addAttribute("hostName", hostName);
        model.addAttribute("servicesOk", servicesOk);
        return "host/services/service-ok";
    }

    @GetMapping("/service-warning/{hostName}")
    public String getServicesWarning(@PathVariable String hostName, Model model) throws Exception {
        List<ServiceInfo> servicesWarning = hostLiveInfoService.getServiceWarnings(hostName);
        model.addAttribute("hostName", hostName);
        model.addAttribute("servicesWarning", servicesWarning);
        return "host/services/service-warning";
    }

    @GetMapping("/service-critical/{hostName}")
    public String getServicesCritical(@PathVariable String hostName, Model model) throws Exception {
        List<ServiceInfo> servicesCritical = hostLiveInfoService.getServiceCriticals(hostName);
        model.addAttribute("hostName", hostName);
        model.addAttribute("servicesCritical", servicesCritical);
        return "host/services/service-critical";
    }

    @PostMapping("/install-agent")
    @ResponseBody
    public ResponseEntity<String> installAgent(
            @RequestParam String host,
            @RequestParam String username,
            @RequestParam String password) {
        try {
            String result = sshService.installCheckmkAgent(host, username, password);
            return ResponseEntity.ok("Agent installation successful." /* + result*/);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error during agent installation: " + e.getMessage());
        }
    }

    private List<LdapUser> getUsersBasedOnRole(LdapUser currentUser) {
        List<String> roles = currentUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        if (roles.contains("ROLE_ADMIN")) {
            return ldapUserService.getAllUsers();
        } else if (roles.contains("ROLE_DEPARTMENTHEAD")) {
            String department = currentUser.getDepartment();
            if (department != null) {
                return ldapUserService.getAllUsers().stream()
                        .filter(user -> department.equals(user.getDepartment()))
                        .collect(Collectors.toList());
            } else {
                return List.of();
            }
        } else if (roles.contains("ROLE_TEAMLEADER")) {
            String team = currentUser.getTeam();
            if (team != null) {
                return ldapUserService.getAllUsers().stream()
                        .filter(user -> team.equals(user.getTeam()))
                        .collect(Collectors.toList());
            } else {

                return List.of();
            }
        }
        return List.of();
    }


}



