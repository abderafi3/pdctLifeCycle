package com.checkmk.pdctLifeCycle.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @GetMapping("/login")
    public String login() {
        return "login";  // Points to src/main/resources/templates/login.html
    }
}
