package com.checkmk.pdctLifeCycle.Controller;

import com.checkmk.pdctLifeCycle.service.SshService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hosts")
public class HostController {

    @Autowired
    private SshService sshService;

    @PostMapping("/install-agent")
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
}
