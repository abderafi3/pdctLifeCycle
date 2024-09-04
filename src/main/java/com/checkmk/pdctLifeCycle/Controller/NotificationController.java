package com.checkmk.pdctLifeCycle.Controller;

import com.checkmk.pdctLifeCycle.model.HostNotification;
import com.checkmk.pdctLifeCycle.model.LdapUser;
import com.checkmk.pdctLifeCycle.service.NotificationService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // Fetch unread notifications for the dropdown
    @GetMapping("/notifications")
    @ResponseBody
    public List<HostNotification> getAllUnreadNotifications(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            LdapUser currentUser = (LdapUser) authentication.getPrincipal();
            // Only fetch unread notifications for the logged-in user, no matter the role
            return notificationService.getUnreadNotifications(currentUser.getEmail());
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
            LdapUser currentUser = (LdapUser) authentication.getPrincipal();
            List<String> roles = currentUser.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_DEPARTMENTHEAD") || roles.contains("ROLE_TEAMLEADER")) {
                return notificationService.getUnreadNotifications(currentUser.getEmail()).size();
            }

            // If it's a regular user, return only their own unread notifications
            return notificationService.getUnreadNotifications(currentUser.getEmail()).size();
        }

        return 0;
    }

    @GetMapping("/notifications/all")
    public String getAllNotifications(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        LdapUser currentUser = (LdapUser) authentication.getPrincipal();
        List<String> roles = currentUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        List<HostNotification> notifications = getNotificationsByRole(currentUser, roles);
        model.addAttribute("pageTitle", "All Notifications");
        model.addAttribute("notifications", notifications);
        model.addAttribute("isAdmin", roles.contains("ROLE_ADMIN")); // Check if the user is an admin

        return "notifications";
    }

    private List<HostNotification> getNotificationsByRole(LdapUser currentUser, List<String> roles) {
        if (roles.contains("ROLE_ADMIN")) {

            return notificationService.getAllNotificationsSortedByDate();
        } else if (roles.contains("ROLE_DEPARTMENTHEAD")) {
            return notificationService.getNotificationsForDepartment(currentUser.getDepartment()); // Department Head sees department's notifications
        } else if (roles.contains("ROLE_TEAMLEADER")) {
            return notificationService.getNotificationsForTeam(currentUser.getTeam());
        } else {
            return notificationService.getAllNotificationsForUser(currentUser.getEmail()); // Regular user sees their own notifications
        }
    }


    // Send a manual notification
    @PostMapping("/sendNotification")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_DEPARTMENTHEAD', 'ROLE_TEAMLEADER')")
    public ResponseEntity<String> sendNotification(@RequestBody Map<String, String> payload, Principal principal) throws MessagingException {
        String userEmail = payload.get("email");
        String title = payload.get("title");
        String message = payload.get("message");
        String hostName = payload.get("hostName");
        String userFullName = payload.get("userFullName");

        String senderName = getSenderNameFromPrincipal(principal);
        notificationService.sendManualNotification(userEmail, title, message, senderName, hostName, userFullName);
        return ResponseEntity.ok("Notification sent successfully!");
    }

    // Helper method to get the sender's  name from the principal
    private String getSenderNameFromPrincipal(Principal principal) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principalObj = authentication.getPrincipal();
        if (principalObj instanceof LdapUser ldapUser) {
            return ldapUser.getFirstName() + " " + ldapUser.getLastName();
        }
        return principal.getName();
    }

    // Delete notification
    @PostMapping("/notifications/delete/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_DEPARTMENTHEAD', 'ROLE_TEAMLEADER')")
    public String deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return "redirect:/notifications/all";
    }

}
