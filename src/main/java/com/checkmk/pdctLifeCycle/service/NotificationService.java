package com.checkmk.pdctLifeCycle.service;

import com.checkmk.pdctLifeCycle.model.Host;
import com.checkmk.pdctLifeCycle.model.HostLiveInfo;
import com.checkmk.pdctLifeCycle.model.HostNotification;
import com.checkmk.pdctLifeCycle.model.LdapUser;
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
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Value("${spring.mail.from}")
    private String fromEmail;

    @Value("${spring.mail.from.name}")
    private String fromName;

    private final NotificationRepository notificationRepository;
    private final HostService hostService;
    private final HostLiveInfoService hostLiveInfoService;
    private final LdapUserService ldapUserService;
    private final JavaMailSender javaMailSender;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository, HostService hostService,
                               HostLiveInfoService hostLiveInfoService,LdapUserService ldapUserService, JavaMailSender javaMailSender){
        this.notificationRepository = notificationRepository;
        this.hostService = hostService;
        this.hostLiveInfoService = hostLiveInfoService;
        this.javaMailSender = javaMailSender;
        this.ldapUserService=ldapUserService;
    }

    private final Map<String, Integer> criticalServiceCountMap = new ConcurrentHashMap<>();

    public List<HostNotification> getAllNotificationsSortedByDate() {
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<HostNotification> getAllNotificationsForUser(String hostUserEmail) {
        return notificationRepository.findByHostUserEmailOrderByCreatedAtDesc(hostUserEmail);
    }

    public List<HostNotification> getUnreadNotifications(String userEmail) {
        return notificationRepository.findByHostUserEmailAndReadFalse(userEmail);
    }

    public List<HostNotification> getNotificationsForDepartment(String department) {
        List<LdapUser> usersInDepartment = ldapUserService.getUsersByDepartment(department);
        List<String> userEmails = usersInDepartment.stream()
                .map(LdapUser::getEmail)
                .collect(Collectors.toList());
        return notificationRepository.findByHostUserEmailInOrderByCreatedAtDesc(userEmails);
    }

    public List<HostNotification> getNotificationsForTeam(String team) {
        List<LdapUser> usersInTeam = ldapUserService.getUsersByTeam(team);
        List<String> userEmails = usersInTeam.stream()
                .map(LdapUser::getEmail)
                .collect(Collectors.toList());
        return notificationRepository.findByHostUserEmailInOrderByCreatedAtDesc(userEmails);
    }


    // Mark a notification as read
    public void markNotificationAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }

    // Create a notification
    public void createNotification(String hostUserEmail, String title, String summary, String createdBy, String hostName, String userFullName) {
        HostNotification hostNotification = new HostNotification(title, summary, hostUserEmail, createdBy, hostName, userFullName);
        notificationRepository.save(hostNotification);
    }

    @Scheduled(cron = "0 0 6 * * ?") // Run every day at 06:00 AM
    public void checkForExpiringHosts() throws MessagingException {
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

    private void sendExpirationWarning(Host host, int daysRemaining) throws MessagingException {
        String hostUserEmail = host.getHostUserEmail();
        String userFullName = host.getHostUser();
        if (hostUserEmail != null && daysRemaining >= 0) {
            String subject = "Host Expiration Warning: " + host.getHostName();
            String summary = "Host " + host.getHostName() + " will expire in " + daysRemaining + " day(s).";

            sendEmailAndCreateNotification(hostUserEmail, subject, summary, "System", host.getHostName(), userFullName, daysRemaining);
        }
    }

    private void sendCriticalServiceNotification(Host host, int oldCount, int newCount) throws MessagingException {
        String hostUserEmail = host.getHostUserEmail();
        String userFullName = host.getHostUser();

        if (hostUserEmail != null) {
            String subject = "Critical Service Alert for Host: " + host.getHostName();
            String summary = "Critical services increased from " + oldCount + " to " + newCount + " for host " + host.getHostName() + ".";

            sendEmailAndCreateNotification(hostUserEmail, subject, summary, "System", host.getHostName(), userFullName, oldCount, newCount);
        }
    }

    public void sendEmail(String to, String userFullName, String subject, String body) throws MessagingException {

        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(fromName + " <" + fromEmail + ">");
        helper.setTo(to);
        helper.setSubject(subject);

        String formattedBody = """
        <div style="font-family: Arial, sans-serif;">
            <p>Dear %s,</p>
            <p>%s</p>
            <p>Best regards,<br>Host Management Team</p>
        </div>
    """.formatted(userFullName, body);

        helper.setText(formattedBody, true);

        javaMailSender.send(message);
    }

    private void sendEmailAndCreateNotification(String hostUserEmail, String subject, String summary, String createdBy, String hostName, String userFullName, int... counts) throws MessagingException {
        // Send email
        sendEmail(hostUserEmail, userFullName, subject, summary);

        // Create a notification
        createNotification(hostUserEmail, subject, summary, createdBy, hostName, userFullName);
    }

    public void sendManualNotification(String email, String title, String summary, String adminName, String hostName, String userFullName) throws MessagingException {
        sendEmailAndCreateNotification(email, title, summary, adminName, hostName, userFullName);
    }

    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }
}
