package com.checkmk.pdctLifeCycle.service;

import com.checkmk.pdctLifeCycle.model.Host;
import com.checkmk.pdctLifeCycle.model.HostLiveInfo;
import com.checkmk.pdctLifeCycle.model.HostNotification;
import com.checkmk.pdctLifeCycle.repository.NotificationRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationService {

    @Value("${spring.mail.from}")
    private String fromEmail;

    @Value("${spring.mail.from.name}")
    private String fromName;

    private final NotificationRepository notificationRepository;
    private final HostService hostService;
    private final HostLiveInfoService hostLiveInfoService;
    private final JavaMailSender javaMailSender;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository, HostService hostService,
                               HostLiveInfoService hostLiveInfoService, JavaMailSender javaMailSender){
        this.notificationRepository = notificationRepository;
        this.hostService = hostService;
        this.hostLiveInfoService = hostLiveInfoService;
        this.javaMailSender = javaMailSender;
    }


    // Store the last known number of critical services for each host
    private final Map<String, Integer> criticalServiceCountMap = new ConcurrentHashMap<>();

    public List<HostNotification> getUnreadNotifications(String hostUserEmail) {
        return notificationRepository.findByHostUserEmailAndReadFalse(hostUserEmail);
    }

    public List<HostNotification> getAllNotificationsForUser(String hostUserEmail) {
        return notificationRepository.findByHostUserEmail(hostUserEmail);
    }

    public void markNotificationAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }

    public void createNotification(String hostUserEmail, String title, String message) {
        HostNotification hostNotification = new HostNotification(title, message, hostUserEmail);
        notificationRepository.save(hostNotification);
    }

    @Scheduled(cron = "0 0 6 * * ?") // Run every day at 06:00 AM
    public void checkForExpiringHosts() {
        LocalDate today = LocalDate.now();
        LocalDate threeDaysFromNow = today.plusDays(3);

        List<Host> hosts = hostService.getAllHosts();

        for (Host host : hosts) {
            String expirationDateString = host.getExpirationDate();
            if (expirationDateString == null || expirationDateString.trim().isEmpty()) {
                continue;
            }
                LocalDate expirationDate = LocalDate.parse(expirationDateString);
                if (!expirationDate.isAfter(threeDaysFromNow)) {
                    int daysRemaining = (int) java.time.temporal.ChronoUnit.DAYS.between(today, expirationDate);
                    sendExpirationWarning(host, daysRemaining);
                }
        }
    }

    @Scheduled(fixedRate = 300000) // Run every 5 minutes to check for any new critical Service
    public void checkForIncreasedCriticalServices() {
        List<Host> hosts = hostService.getAllHosts();

        for (Host host : hosts) {
            try {
                HostLiveInfo liveInfo = hostLiveInfoService.getLiveInfoForHost(host.getHostName());
                if (liveInfo != null) {
                    int currentCriticalServices = Integer.parseInt(liveInfo.getServiceCritical());
                    int lastKnownCriticalServices = criticalServiceCountMap.getOrDefault(host.getHostName(), 0);

                    if (currentCriticalServices > lastKnownCriticalServices) {
                        sendCriticalServiceNotification(host, lastKnownCriticalServices, currentCriticalServices);
                        criticalServiceCountMap.put(host.getHostName(), currentCriticalServices);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error processing host " + host.getHostName() + ": " + e.getMessage());
            }
        }
    }

    private void sendExpirationWarning(Host host, int daysRemaining) {
        String hostUserEmail = host.getHostUserEmail();

        if (hostUserEmail != null && daysRemaining >= 0) {
            String subject = "Host Expiration Warning: " + host.getHostName();
            String message = String.format("Dear %s,<br><br>Your host <b>%s</b> will expire in <b>%d</b> day(s).<br>" +
                            "Please take the necessary action.<br><br>Best regards,<br>Host Management Team",
                    hostUserEmail, host.getHostName(), daysRemaining);

            sendEmailAndCreateNotification(hostUserEmail, subject, message);
        }
    }

    private void sendCriticalServiceNotification(Host host, int oldCount, int newCount) {
        String hostUserEmail = host.getHostUserEmail();

        if (hostUserEmail != null) {
            String subject = "Critical Service Alert for Host: " + host.getHostName();
            String message = String.format("Dear %s,<br><br>The number of critical services for your host <b>%s</b> has increased.<br>" +
                            "Previous count: <b>%d</b><br>Current count: <b>%d</b><br><br>" +
                            "Please check the host and resolve the issues.<br><br>Best regards,<br>Host Management Team",
                    hostUserEmail, host.getHostName(), oldCount, newCount);

            sendEmailAndCreateNotification(hostUserEmail, subject, message);
        }
    }

    public void sendEmail(String to, String subject, String text) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromName + " <" + fromEmail + ">");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, true);

            javaMailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    private void sendEmailAndCreateNotification(String hostUserEmail, String subject, String messageBody) {
        try {
            sendEmail(hostUserEmail, subject, messageBody);
            createNotification(hostUserEmail, subject, messageBody);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String sendManualNotification(String email, String title, String messageBody, String hostUserEmail) {
        try {
            sendEmailAndCreateNotification(hostUserEmail, title, messageBody);
            return "Notification sent successfully!";
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to send notification.";
        }
    }
}
