package com.checkmk.pdctLifeCycle.Controller;

import com.checkmk.pdctLifeCycle.exception.HostServiceException;
import com.checkmk.pdctLifeCycle.model.Host;
import com.checkmk.pdctLifeCycle.model.HostWithLiveInfo;
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
    private HostLiveInfoService hostLiveInfoService;

    @GetMapping
    public String getAllHosts(Model model) {
        List<Host> hosts = hostService.getAllHosts();
        model.addAttribute("hosts", hosts);
        model.addAttribute("pageTitle", "Host List");
        return "host/list"; // Maps to src/main/resources/templates/host/list.html
    }

    @GetMapping("/{id}")
    public String getHostById(@PathVariable String id, Model model) {
        Host host = hostService.getHostById(id);
        model.addAttribute("host", host);
        model.addAttribute("pageTitle", "Host Details");
        return "host/detail"; // Maps to src/main/resources/templates/host/detail.html
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
        model.addAttribute("host", host);
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

    @GetMapping("/info")
    public String getHostsWithLiveInfo(Model model) throws Exception {
        List<Host> hosts = hostService.getAllHosts();
        List<HostWithLiveInfo> combinedHostInfo = hosts.stream().map(host -> {
            HostWithLiveInfo hostWithLiveInfo = new HostWithLiveInfo();
            hostWithLiveInfo.setHost(host);
            try {
                hostWithLiveInfo.setLiveInfo(hostLiveInfoService.convertToHostLiveInfo().stream()
                        .filter(info -> info.getHostName().equals(host.getHostName()))
                        .findFirst()
                        .orElse(null));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return hostWithLiveInfo;
        }).collect(Collectors.toList());

        model.addAttribute("combinedHostInfo", combinedHostInfo);
        model.addAttribute("pageTitle", "Hosts with Live Info");
        return "host/host-list"; // Maps to src/main/resources/templates/host/host-list.html
    }

    @GetMapping("/import")
    public String importHosts(Model model) {
        List<Host> checkmkHosts = hostService.getCheckMkHosts();
        List<Host> dbHosts = hostService.getAllHosts();
        model.addAttribute("checkmkHosts", checkmkHosts);
        model.addAttribute("dbHosts", dbHosts);
        model.addAttribute("pageTitle", "Import Hosts");
        return "host/import"; // Maps to src/main/resources/templates/host/import.html
    }

    @PostMapping("/import")
    public String saveImportedHosts(@RequestParam("selectedHostIds") List<String> selectedHostIds, Model model) {
        hostService.saveSelectedHosts(selectedHostIds);
        model.addAttribute("message", "Hosts imported successfully");
        return "redirect:/hosts/import";
    }
}
