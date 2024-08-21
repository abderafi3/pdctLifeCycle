package com.checkmk.pdctLifeCycle.Controller;

import com.checkmk.pdctLifeCycle.exception.HostServiceException;
import com.checkmk.pdctLifeCycle.model.Host;
import com.checkmk.pdctLifeCycle.model.HostLiveInfo;
import com.checkmk.pdctLifeCycle.model.HostWithLiveInfo;
import com.checkmk.pdctLifeCycle.model.LdapUser;
import com.checkmk.pdctLifeCycle.service.HostImportService;
import com.checkmk.pdctLifeCycle.service.HostLiveInfoService;
import com.checkmk.pdctLifeCycle.service.HostService;
import com.checkmk.pdctLifeCycle.service.LdapUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.ldap.userdetails.LdapUserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/hosts")
public class HostWebController {

    private final HostService hostService;
    private final HostImportService hostImportService;
    private final HostLiveInfoService hostLiveInfoService;
    private final LdapUserService ldapUserService;

    @Autowired
    public HostWebController(HostService hostService, HostImportService hostImportService, HostLiveInfoService hostLiveInfoService, LdapUserService ldapUserService){
        this.hostService = hostService;
        this.hostImportService = hostImportService;
        this.hostLiveInfoService = hostLiveInfoService;
        this.ldapUserService = ldapUserService;
    }


    @GetMapping
    public String getHostsWithLiveInfo(Model model) throws Exception {
        List<Host> hosts;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            LdapUserDetails userDetails = (LdapUserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

            if (isAdmin) {
                hosts = hostService.getAllHosts(); // Admin gets all hosts
            } else {
                // Non-admin users get only their own hosts, filtering by the LDAP username
                hosts = hostService.getHostsByUsername(username);
            }
        } else {
            hosts = new ArrayList<>();
        }

        List<HostWithLiveInfo> combinedHostInfo = hosts.stream().map(host -> {
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

        model.addAttribute("combinedHostInfo", combinedHostInfo);
        model.addAttribute("pageTitle", "Hosts with Live Info");
        return "host/list";
    }

    @GetMapping("/add")
    public String showAddHostForm(Model model) {
        model.addAttribute("host", new Host());
        model.addAttribute("pageTitle", "Add Host");
        return "host/add";
    }

    @PostMapping("/add")
    public String addHost(@ModelAttribute Host host) throws HostServiceException {
        hostService.addHost(host);
        return "redirect:/hosts";
    }

    @GetMapping("/edit/{id}")
    public String showEditHostForm(@PathVariable String id, Model model) {
        Host host = hostService.getHostById(id);
        List<LdapUser> users = ldapUserService.getAllUsers();
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
    public String saveImportedHosts(@RequestParam("selectedHostIds") List<String> selectedHostIds, Model model) {
        hostImportService.saveSelectedHosts(selectedHostIds);
        return "redirect:/hosts/import";
    }
}
