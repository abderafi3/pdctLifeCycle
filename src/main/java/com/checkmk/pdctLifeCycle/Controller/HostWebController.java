package com.checkmk.pdctLifeCycle.Controller;

import com.checkmk.pdctLifeCycle.exception.HostServiceException;
import com.checkmk.pdctLifeCycle.model.Host;
import com.checkmk.pdctLifeCycle.model.HostWithLiveInfo;
import com.checkmk.pdctLifeCycle.model.LdapUser;
import com.checkmk.pdctLifeCycle.service.HostImportService;
import com.checkmk.pdctLifeCycle.service.HostService;
import com.checkmk.pdctLifeCycle.service.LdapUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/hosts")
public class HostWebController {

    private final HostService hostService;
    private final HostImportService hostImportService;
    private final LdapUserService ldapUserService;

    @Autowired
    public HostWebController(HostService hostService, HostImportService hostImportService, LdapUserService ldapUserService) {
        this.hostService = hostService;
        this.hostImportService = hostImportService;
        this.ldapUserService = ldapUserService;
    }

    @GetMapping
    public String getHostsPage(Model model, Authentication authentication) throws Exception {
        List<String> roles = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        model.addAttribute("userRoles", roles);
        model.addAttribute("pageTitle", "Hosts with Live Info");
        model.addAttribute("userRoles", authentication.getAuthorities());
        return "host/list";
    }

    @GetMapping("/live-data")
    @ResponseBody
    public List<HostWithLiveInfo> getLiveHostData(Authentication authentication) throws Exception {
        return hostService.getHostsWithLiveInfo(authentication);
    }

    @GetMapping("/add")
    public String showAddHostForm(Model model) {
        model.addAttribute("host", new Host());
        model.addAttribute("pageTitle", "Add Host");
        return "host/add";
    }

    @PostMapping("/add")
    public String addHost(@ModelAttribute Host host, RedirectAttributes redirectAttributes) {
        try {
            hostService.addHost(host);
            redirectAttributes.addFlashAttribute("successMessage", "Host added successfully!");
            return "redirect:/hosts";  // Redirect to the list of hosts on success

        } catch (HostServiceException e) {
            // Log the error for further inspection
            e.printStackTrace();  // Or use a logger

            // Add error message to be displayed to the user
            redirectAttributes.addFlashAttribute("errorMessage", "Couldn't add the host: " + e.getMessage());

            // Redirect back to the add page with the error message
            return "redirect:/hosts/add";
        }
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
}
