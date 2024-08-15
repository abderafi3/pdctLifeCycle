package com.checkmk.pdctLifeCycle.Controller;

import com.checkmk.pdctLifeCycle.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @PostMapping("/sendNotification")
    @ResponseBody
    public String sendNotification(@RequestBody Map<String, String> payload) {
        try {
            String email = payload.get("email");
            String title = payload.get("title");
            String message = payload.get("message");

            // Use the NotificationService to send the email
            return notificationService.sendManualNotification(email, title, message);
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to send notification.";
        }
    }
}
