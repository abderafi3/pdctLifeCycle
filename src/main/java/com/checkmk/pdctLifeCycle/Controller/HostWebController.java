package com.checkmk.pdctLifeCycle.Controller;

import com.checkmk.pdctLifeCycle.exception.HostServiceException;
import com.checkmk.pdctLifeCycle.model.Host;
import com.checkmk.pdctLifeCycle.model.HostLiveInfo;
import com.checkmk.pdctLifeCycle.model.HostUser;
import com.checkmk.pdctLifeCycle.model.HostWithLiveInfo;
import com.checkmk.pdctLifeCycle.service.HostImportService;
import com.checkmk.pdctLifeCycle.service.HostLiveInfoService;
import com.checkmk.pdctLifeCycle.service.HostService;
import com.checkmk.pdctLifeCycle.service.UsersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/hosts")
public class HostWebController {

    @Autowired
    private HostService hostService;

    @Autowired
    private HostImportService hostImportService;

    @Autowired
    private HostLiveInfoService hostLiveInfoService;

    @Autowired
    private UsersService usersService;


    // Admin access: View all hosts with live info
    @Secured("ROLE_ADMIN")
    @GetMapping
    public String getHostsWithLiveInfo(Model model) throws Exception {
        List<Host> hosts = hostService.getAllHosts();
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
        return "host/list"; // Maps to src/main/resources/templates/host/list.html
    }

    // User access: View only the user's assigned hosts
    @Secured("ROLE_USER")
    @GetMapping("/myhosts")
    public String getUserHostsWithLiveInfo(Model model, Principal principal) throws Exception {
        String email = principal.getName(); // Get current user's email
        HostUser user = usersService.getUserByEmail(email);

        List<Host> hosts = hostService.getHostsByUser(user); // Fetch only hosts assigned to this user
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
        model.addAttribute("pageTitle", "My Hosts with Live Info");
        return "host/myhosts"; // Maps to src/main/resources/templates/host/myhosts.html
    }

    // Admin access: Show form to add new host
    @Secured("ROLE_ADMIN")
    @GetMapping("/add")
    public String showAddHostForm(Model model) {
        model.addAttribute("host", new Host());
        model.addAttribute("pageTitle", "Add Host");
        return "host/add"; // Maps to src/main/resources/templates/host/add.html
    }

    // Admin access: Add new host
    @Secured("ROLE_ADMIN")
    @PostMapping("/add")
    public String addHost(@ModelAttribute Host host) throws HostServiceException {
        hostService.addHost(host);
        return "redirect:/hosts";
    }

    // Admin access: Show form to edit a host
    @Secured("ROLE_ADMIN")
    @GetMapping("/edit/{id}")
    public String showEditHostForm(@PathVariable String id, Model model) {
        Host host = hostService.getHostById(id);
        List<HostUser> users = usersService.getAllUsers();  // Fetch all users from the database

        model.addAttribute("host", host);
        model.addAttribute("users", users);  // Pass the users to the model
        model.addAttribute("pageTitle", "Edit Host");
        return "host/edit"; // Maps to src/main/resources/templates/host/edit.html
    }

    // Admin access: Update host
    @Secured("ROLE_ADMIN")
    @PostMapping("/edit/{id}")
    public String updateHost(@PathVariable String id, @ModelAttribute Host host, @RequestParam("user") String email) throws HostServiceException {
        HostUser selectedUser = usersService.getUserByEmail(email);  // Fetch the selected user by email
        host.setHostUser(selectedUser);  // Reassign the selected user to the host
        host.setId(id);
        hostService.updateHost(host);
        return "redirect:/hosts";
    }

    // Admin access: Delete host
    @Secured("ROLE_ADMIN")
    @GetMapping("/delete/{id}")
    public String deleteHost(@PathVariable String id) throws HostServiceException {
        hostService.deleteHost(id);
        return "redirect:/hosts";
    }

    // Admin access: Import hosts from external service
    @Secured("ROLE_ADMIN")
    @GetMapping("/import")
    public String importHosts(Model model) {
        List<Host> checkmkHosts = hostImportService.getCheckMkHosts();
        List<Host> dbHosts = (List<Host>) hostService.getAllHosts();
        model.addAttribute("checkmkHosts", checkmkHosts);
        model.addAttribute("dbHosts", dbHosts);
        model.addAttribute("pageTitle", "Import Hosts");
        return "host/import"; // Maps to src/main/resources/templates/host/import.html
    }

    // Admin access: Save imported hosts
    @Secured("ROLE_ADMIN")
    @PostMapping("/import")
    public String saveImportedHosts(@RequestParam("selectedHostIds") List<String> selectedHostIds, Model model) {
        hostImportService.saveSelectedHosts(selectedHostIds);
        model.addAttribute("message", "Hosts imported successfully");
        return "redirect:/hosts/import";
    }
}
