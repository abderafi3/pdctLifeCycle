package com.checkmk.pdctLifeCycle.service;

import com.checkmk.pdctLifeCycle.config.CheckmkConfig;
import com.checkmk.pdctLifeCycle.model.HostNotification;
import com.checkmk.pdctLifeCycle.repository.NotificationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(HostService.class);

        private final NotificationRepository notificationRepository;
        private final CheckmkConfig checkmkConfig;
        private final RestClientService restClientService;

        @Autowired
        public NotificationService(NotificationRepository notificationRepository, CheckmkConfig checkmkConfig, RestClientService restClientService) {
            this.notificationRepository = notificationRepository;
            this.checkmkConfig = checkmkConfig;
            this.restClientService = restClientService;
        }

        public List<HostNotification> getAllHostNotifications() {
            return notificationRepository.findAll();
        }

        public HostNotification getHostNotificationById(String id) {
            return notificationRepository.findById(id).orElse(null);
        }

        public HostNotification saveNotification(HostNotification hostNotification) {
            return notificationRepository.save(hostNotification);
        }

        public void deleteNotification(String id) {
            notificationRepository.deleteById(id);
        }

    public List<HostNotification> fetchNotificationsFromCheckmk() throws IOException {
        String apiUrl = checkmkConfig.getApiUrl() + "/view.py?output_format=json_export&view_name=notifications";
        ResponseEntity<String> response = restClientService.sendGetRequest(apiUrl, String.class);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(response.getBody());

        List<HostNotification> notifications = new ArrayList<>();
        Iterator<JsonNode> elements = rootNode.elements();

        // Read the headers
        JsonNode headersNode = elements.next();
        List<String> headers = new ArrayList<>();
        headersNode.forEach(header -> headers.add(header.asText()));

        // Process the data
        while (elements.hasNext()) {
            JsonNode node = elements.next();
            HostNotification notification = new HostNotification();
            for (int i = 0; i < headers.size(); i++) {
                String header = headers.get(i);
                JsonNode valueNode = node.get(i);

                switch (header) {
                    case "log_type":
                        notification.setType(valueNode.asText());
                        break;
                    case "log_plugin_output":
                        notification.setMessage(valueNode.asText());
                        break;
                    case "log_time":
                        notification.setCreatedAt(valueNode.asText());
                        break;
                    case "log_state":
                        notification.setSeverity(valueNode.asText());
                        break;
                    default:
                        // Handle other headers if needed
                        break;
                }
            }
            notifications.add(notification);
        }
        return notifications;
    }
}
