package com.checkmk.pdctLifeCycle.Controller;

import com.checkmk.pdctLifeCycle.model.HostNotification;
import com.checkmk.pdctLifeCycle.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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

    // Fetch unread notifications for the dropdown
    @GetMapping("/notifications")
    @ResponseBody
    public List<HostNotification> getNotifications(Principal principal) {
        if (principal != null) {
            String username = principal.getName(); // Get the username from LDAP
            List<HostNotification> notifications = notificationService.getUnreadNotifications(username);
            return notifications;  // Return the list of notifications
        }
        return List.of();  // Return an empty list if no principal is present
    }

    // Mark a notification as read
    @PostMapping("/notifications/read/{id}")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public String markAsRead(@PathVariable Long id) {
        try {
            notificationService.markNotificationAsRead(id);
            return "Notification marked as read.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to mark notification as read.";
        }
    }

    // Fetch unread notifications count for the navbar
    @GetMapping("/notifications/unread-count")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public int getUnreadNotificationCount(Principal principal) {
        if (principal != null) {
            String username = principal.getName(); // Get the username from LDAP
            return notificationService.getUnreadNotifications(username).size();
        }
        return 0; // Return 0 if no principal is present
    }

    // Fetch all notifications for the user
    @GetMapping("/notifications/all")
    public String getAllNotifications(Principal principal, Model model) {
        if (principal != null) {
            String username = principal.getName(); // Get the username from LDAP
            List<HostNotification> notifications = notificationService.getAllNotificationsForUser(username);
            model.addAttribute("pageTitle", "All Notifications");
            model.addAttribute("notifications", notifications);
            return "notifications"; // Maps to notifications.html
        }
        return "redirect:/login"; // Redirect to login if the user is not authenticated
    }

    // Send a manual notification (Admin Only)
    @PostMapping("/sendNotification")
    @ResponseBody
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String sendNotification(@RequestBody Map<String, String> payload) {
        try {
            String username = payload.get("username");  // Using username instead of email
            String title = payload.get("title");
            String message = payload.get("message");

            if (username == null || username.isEmpty()) {
                return "Username not provided.";
            }

            // Send the manual notification using the username
            notificationService.sendManualNotification(username + "@asagno.local", title, message, username);
            return "Notification sent successfully!";
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to send notification.";
        }
    }
}
