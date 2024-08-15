package com.checkmk.pdctLifeCycle.Controller;

import com.checkmk.pdctLifeCycle.model.HostNotification;
import com.checkmk.pdctLifeCycle.model.HostUser;
import com.checkmk.pdctLifeCycle.service.NotificationService;
import com.checkmk.pdctLifeCycle.service.UsersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UsersService usersService;

    @GetMapping("/notifications")
    public String getNotifications(Principal principal, Model model) {
        String email = principal.getName();
        HostUser user = usersService.getUserByEmail(email);

        // Fetch notifications for this user
        List<HostNotification> notifications = notificationService.getAllNotificationsForUser(user);

        model.addAttribute("notifications", notifications);
        return "notifications"; // points to notifications.html
    }

    @ModelAttribute("unreadNotifications")
    public List<HostNotification> addUnreadNotificationsToModel(Principal principal) {
        if (principal != null) {
            String email = principal.getName();
            HostUser user = usersService.getUserByEmail(email);
            return notificationService.getUnreadNotifications(user);
        }
        return List.of(); // Return an empty list if the user is not logged in
    }

    @PostMapping("/notifications/read/{id}")
    @ResponseBody
    public void markAsRead(@PathVariable Long id) {
        notificationService.markNotificationAsRead(id);
    }

    @PostMapping("/sendNotification")
    @ResponseBody
    public String sendNotification(@RequestBody Map<String, String> payload) {
        try {
            String email = payload.get("email");
            String title = payload.get("title");
            String message = payload.get("message");

            // Fetch the user by email
            HostUser user = usersService.getUserByEmail(email);
            if (user == null) {
                return "User not found.";
            }

            // Use the NotificationService to send the email and store the notification
            return notificationService.sendManualNotification(email, title, message, user);
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to send notification.";
        }
    }
}
