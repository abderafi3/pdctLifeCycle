package com.checkmk.pdctLifeCycle.Controller;

import com.checkmk.pdctLifeCycle.model.HostNotification;
import com.checkmk.pdctLifeCycle.model.LdapUser;
import com.checkmk.pdctLifeCycle.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
            String username = principal.getName();
            List<HostNotification> notifications = notificationService.getUnreadNotifications(username);
            return notifications;
        }
        return List.of();
    }

    // Mark a notification as read
    @PostMapping("/notifications/read/{id}")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public void markAsRead(@PathVariable Long id) {
           notificationService.markNotificationAsRead(id);
    }


    @GetMapping("/notifications/unread-count")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public int getUnreadNotificationCount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();

            if (principal instanceof LdapUser) {
                LdapUser ldapUser = (LdapUser) principal;
                String userEmail = ldapUser.getEmail();

                return notificationService.getUnreadNotifications(userEmail).size();
            }
        }

        return 0; // Return 0 if no principal or email is found
    }

    // Fetch all notifications for the user
    @GetMapping("/notifications/all")
    public String getAllNotifications(Principal principal, Model model) {
        if (principal != null) {
            String userEmail = principal.getName(); // Get the user's email (userPrincipalName from LDAP)
            List<HostNotification> notifications = notificationService.getAllNotificationsForUser(userEmail);
            model.addAttribute("pageTitle", "All Notifications");
            model.addAttribute("notifications", notifications);
            return "notifications";
        }
        return "redirect:/login"; // Redirect to login if the user is not authenticated
    }

    // Send a manual notification (Admin Only)
    @PostMapping("/sendNotification")
    @ResponseBody
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String sendNotification(@RequestBody Map<String, String> payload) {
        try {
            String userEmail = payload.get("email");
            String title = payload.get("title");
            String message = payload.get("message");

            if (userEmail == null || userEmail.isEmpty()) {
                return "Email not provided.";
            }

            // Send the manual notification using the email
            notificationService.sendManualNotification(userEmail, title, message, userEmail);
            return "Notification sent successfully!";
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to send notification.";
        }
    }
}
