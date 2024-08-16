package com.checkmk.pdctLifeCycle.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("pageTitle", "Login");
        return "login";  // Points to src/main/resources/templates/login.html
    }
}
