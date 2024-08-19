package com.checkmk.pdctLifeCycle.Controller;

import com.checkmk.pdctLifeCycle.model.HostNotification;
import com.checkmk.pdctLifeCycle.model.HostUser;
import com.checkmk.pdctLifeCycle.service.NotificationService;
import com.checkmk.pdctLifeCycle.service.UsersService;
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

    @Autowired
    private UsersService usersService;

    // Fetch unread notifications for the dropdown
    @GetMapping("/notifications")
    @ResponseBody
    public List<HostNotification> getNotifications(Principal principal) {
        String email = principal.getName();
        HostUser user = usersService.getUserByEmail(email);

        // Ensure that we return a list of notifications
        List<HostNotification> notifications = notificationService.getUnreadNotifications(user);

        return notifications;  // This should return a List, not a single object
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
            String email = principal.getName();
            HostUser user = usersService.getUserByEmail(email);
            return notificationService.getUnreadNotifications(user).size();
        }
        return 0;
    }

    @GetMapping("/notifications/all")
    public String getAllNotifications(Principal principal, Model model) {
        if (principal != null) {
            String email = principal.getName();
            HostUser user = usersService.getUserByEmail(email);

            // Fetch all notifications for this user
            List<HostNotification> notifications = notificationService.getAllNotificationsForUser(user);
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
            String email = payload.get("email");
            String title = payload.get("title");
            String message = payload.get("message");

            // Fetch the user by email
            HostUser user = usersService.getUserByEmail(email);
            if (user == null) {
                return "User not found.";
            }

            // Use the NotificationService to send the email and store the notification
            notificationService.sendManualNotification(email, title, message, user);
            return "Notification sent successfully!";
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to send notification.";
        }
    }
}
