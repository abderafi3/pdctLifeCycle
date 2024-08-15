package com.checkmk.pdctLifeCycle.service;

import com.checkmk.pdctLifeCycle.model.Host;
import com.checkmk.pdctLifeCycle.model.HostLiveInfo;
import com.checkmk.pdctLifeCycle.model.HostNotification;
import com.checkmk.pdctLifeCycle.model.HostUser;
import com.checkmk.pdctLifeCycle.repository.NotificationRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private HostService hostService;

    @Autowired
    private HostLiveInfoService hostLiveInfoService;

    @Autowired
    private JavaMailSender mailSender;

    private final String fromName = "Host Management Team";
    private final String fromEmail = "hostManagementTeam@asagno.com";

    // Store last known number of critical services for each host
    private final Map<String, Integer> criticalServiceCountMap = new ConcurrentHashMap<>();

    public List<HostNotification> getUnreadNotifications(HostUser user) {
        return notificationRepository.findByUserAndReadFalse(user);
    }

    public List<HostNotification> getAllNotificationsForUser(HostUser user) {
        return notificationRepository.findByUser(user);
    }

    public void markNotificationAsRead(Long notificationId) {
        HostNotification hostNotification = notificationRepository.findById(notificationId).orElseThrow();
        hostNotification.setRead(true);
        notificationRepository.save(hostNotification);
    }

    public void createNotification(HostUser user, String title, String message) {
        HostNotification hostNotification = new HostNotification();
        hostNotification.setTitle(title);
        hostNotification.setMessage(message);
        hostNotification.setUser(user);
        hostNotification.setCreatedAt(LocalDateTime.now());
        hostNotification.setRead(false);
        notificationRepository.save(hostNotification);
    }

    @Scheduled(cron = "0 0 6 * * ?") // Run every day at 06:00 AM
    public void checkForExpiringHosts() {
        LocalDate today = LocalDate.now();
        LocalDate threeDaysFromNow = today.plusDays(3);
        LocalDate oneDayFromNow = today.plusDays(1);

        // Fetch all hosts
        List<Host> hosts = hostService.getAllHosts();

        // Loop through hosts and check if they are expiring soon
        for (Host host : hosts) {
            try {
                // Ensure the expiration date is not null or empty
                String expirationDateString = host.getExpirationDate();
                if (expirationDateString == null || expirationDateString.trim().isEmpty()) {
                    continue;
                }

                LocalDate expirationDate = LocalDate.parse(expirationDateString);

                // Send notification if 3 days or less before expiration
                if (!expirationDate.isAfter(threeDaysFromNow)) {
                    int daysRemaining = (int) java.time.temporal.ChronoUnit.DAYS.between(today, expirationDate);
                    sendExpirationWarning(host, daysRemaining);
                }
            } catch (DateTimeParseException e) {
                System.out.println("Failed to parse expiration date for host " + host.getHostName() + ": " + e.getMessage());
            }
        }
    }

    // Schedule critical service check every 5 minutes
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void checkForIncreasedCriticalServices() throws Exception {
        // Fetch all hosts
        List<Host> hosts = hostService.getAllHosts();

        // Loop through hosts and check their live info
        for (Host host : hosts) {
            HostLiveInfo liveInfo = hostLiveInfoService.getLiveInfoForHost(host.getHostName());

            if (liveInfo != null) {
                int currentCriticalServices = Integer.parseInt(liveInfo.getServiceCritical());

                // Get the last known critical service count (defaults to 0 if not present)
                int lastKnownCriticalServices = criticalServiceCountMap.getOrDefault(host.getHostName(), 0);

                // Check if the number of critical services has increased
                if (currentCriticalServices > lastKnownCriticalServices) {
                    // Send notification to the user
                    sendCriticalServiceNotification(host, lastKnownCriticalServices, currentCriticalServices);

                    // Update the last known critical services count
                    criticalServiceCountMap.put(host.getHostName(), currentCriticalServices);
                }
            }
        }
    }

    private void sendExpirationWarning(Host host, int daysRemaining) {
        HostUser user = host.getHostUser();

        if (user != null && daysRemaining >= 0) {
            // Construct email message
            String subject = "Host Expiration Warning: " + host.getHostName();
            String message = "Dear " + user.getFirstName() + ",<br><br>" +
                    "Your host <b>" + host.getHostName() + "</b> will expire in <b>" + daysRemaining + "</b> day(s).<br>" +
                    "Please take the necessary action.<br><br>" +
                    "Best regards,<br>" +
                    "Host Management Team";

            // Send email
            sendEmail(user.getEmail(), subject, message);

            // Save notification in the database
            createNotification(user, subject, message);
        }
    }

    private void sendCriticalServiceNotification(Host host, int oldCount, int newCount) {
        HostUser user = host.getHostUser();

        if (user != null) {
            // Construct email message
            String subject = "Critical Service Alert for Host: " + host.getHostName();
            String message = "Dear " + user.getFirstName() + ",<br><br>" +
                    "The number of critical services for your host <b>" + host.getHostName() + "</b> has increased.<br>" +
                    "Previous count: <b>" + oldCount + "</b><br>" +
                    "Current count: <b>" + newCount + "</b><br><br>" +
                    "Please check the host and resolve the issues.<br><br>" +
                    "Best regards,<br>" +
                    "Host Management Team";

            // Send email
            sendEmail(user.getEmail(), subject, message);

            // Save notification in the database
            createNotification(user, subject, message);
        }
    }

    public void sendEmail(String to, String subject, String text) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromName + " <" + fromEmail + ">");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    // Sending manual notifications and storing them in the database
    public String sendManualNotification(String email, String title, String messageBody, HostUser user) {
        try {
            sendEmail(email, title, messageBody);
            createNotification(user, title, messageBody); // Store the manual notification
            return "Notification sent and stored successfully!";
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to send and store notification.";
        }
    }
}
