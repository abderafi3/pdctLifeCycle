package com.checkmk.pdctLifeCycle.Controller;

import com.checkmk.pdctLifeCycle.exception.HostServiceException;
import com.checkmk.pdctLifeCycle.model.Host;
import com.checkmk.pdctLifeCycle.model.HostLiveInfo;
import com.checkmk.pdctLifeCycle.model.HostWithLiveInfo;
import com.checkmk.pdctLifeCycle.service.HostImportService;
import com.checkmk.pdctLifeCycle.service.HostLiveInfoService;
import com.checkmk.pdctLifeCycle.service.HostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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

//    @Autowired
//    private UserService userService; // UserService to fetch users

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

    @GetMapping("/add")
    public String showAddHostForm(Model model) {
        model.addAttribute("host", new Host());
        model.addAttribute("pageTitle", "Add Host");
        return "host/add"; // Maps to src/main/resources/templates/host/add.html
    }

    @PostMapping("/add")
    public String addHost(@ModelAttribute Host host) throws HostServiceException {
        hostService.addHost(host);
        return "redirect:/hosts";
    }

    @GetMapping("/edit/{id}")
    public String showEditHostForm(@PathVariable String id, Model model) {
        Host host = hostService.getHostById(id);
        //List<User> users = userService.getAllUsers(); // Fetch all users
        model.addAttribute("host", host);
        //model.addAttribute("users", users);
        model.addAttribute("pageTitle", "Edit Host");
        return "host/edit"; // Maps to src/main/resources/templates/host/edit.html
    }

    @PostMapping("/edit/{id}")
    public String updateHost(@PathVariable String id, @ModelAttribute Host host) throws HostServiceException {
        host.setId(id);
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
        List<Host> dbHosts = (List<Host>) hostService.getAllHosts();
        model.addAttribute("checkmkHosts", checkmkHosts);
        model.addAttribute("dbHosts", dbHosts);
        model.addAttribute("pageTitle", "Import Hosts");
        return "host/import"; // Maps to src/main/resources/templates/host/import.html
    }

    @PostMapping("/import")
    public String saveImportedHosts(@RequestParam("selectedHostIds") List<String> selectedHostIds, Model model) {
        hostImportService.saveSelectedHosts(selectedHostIds);
        model.addAttribute("message", "Hosts imported successfully");
        return "redirect:/hosts/import";
    }
}
