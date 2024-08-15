package com.checkmk.pdctLifeCycle.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class NotificationController {

//    @Autowired
//    private JavaMailSender mailSender;

    @PostMapping("/sendNotification")
    @ResponseBody
    public String sendNotification(@RequestBody Map<String, String> payload) {
        try {
            String email = payload.get("email");
            String title = payload.get("title");
            String message = payload.get("message");

//            // Prepare the email
//            SimpleMailMessage mailMessage = new SimpleMailMessage();
//            mailMessage.setTo(email);
//            mailMessage.setSubject(title);
//            mailMessage.setText(message);
//
//            // Send the email
//            mailSender.send(mailMessage);

            return "Notification sent successfully!";
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to send notification.";
        }
    }
}
