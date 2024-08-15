package com.checkmk.pdctLifeCycle.service;

import com.checkmk.pdctLifeCycle.config.CheckmkConfig;
import com.checkmk.pdctLifeCycle.model.HostLiveInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class HostLiveInfoService {

    private final RestClientService restClientService;
    private final CheckmkConfig checkmkConfig;
    private final ObjectMapper objectMapper;

    @Autowired
    public HostLiveInfoService(RestClientService restClientService, CheckmkConfig checkmkConfig, ObjectMapper objectMapper) {
        this.restClientService = restClientService;
        this.checkmkConfig = checkmkConfig;
        this.objectMapper = objectMapper;
    }

    // Fetch all live info for hosts
    public List<HostLiveInfo> convertToHostLiveInfo() throws Exception {
        String jsonData = fetchHostsLiveInfoFromCheckmk();
        return parseHostLiveInfo(jsonData);
    }

    // Fetch live info for a specific host by hostname
    public HostLiveInfo getLiveInfoForHost(String hostName) throws Exception {
        List<HostLiveInfo> allHostsLiveInfo = convertToHostLiveInfo();
        return allHostsLiveInfo.stream()
                .filter(hostLiveInfo -> hostLiveInfo.getHostName().equalsIgnoreCase(hostName))
                .findFirst()
                .orElse(null);  // Return null if not found, you may handle this case separately
    }

    // Fetch raw data from Checkmk
    private String fetchHostsLiveInfoFromCheckmk() {
        String apiUrl = checkmkConfig.getApiUrl() + "/view.py?output_format=json_export&view_name=allhosts";
        ResponseEntity<String> response = restClientService.sendGetRequest(apiUrl, String.class);
        return response.getBody();
    }

    // Parse JSON response to a list of HostLiveInfo objects
    private List<HostLiveInfo> parseHostLiveInfo(String jsonData) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(jsonData);
        Iterator<JsonNode> elements = jsonNode.elements();
        List<String> keys = new ArrayList<>();

        // Parse the keys from the first JSON element (header row)
        if (elements.hasNext()) {
            JsonNode firstRow = elements.next();
            firstRow.forEach(keyNode -> keys.add(keyNode.asText()));
        }

        List<HostLiveInfo> hostLiveInfoList = new ArrayList<>();

        // Parse the host data
        while (elements.hasNext()) {
            JsonNode valuesNode = elements.next();
            HostLiveInfo hostLiveInfo = new HostLiveInfo();

            // Fill the HostLiveInfo object
            for (int i = 0; i < keys.size(); i++) {
                String key = keys.get(i);
                JsonNode valueNode = valuesNode.get(i);

                switch (key) {
                    case "host":
                        hostLiveInfo.setHostName(valueNode.asText());
                        break;
                    case "host_state":
                        hostLiveInfo.setHostState(valueNode.asText());
                        break;
                    case "num_services_ok":
                        hostLiveInfo.setServiceOk(valueNode.asText());
                        break;
                    case "num_services_warn":
                        hostLiveInfo.setServiceWarning(valueNode.asText());
                        break;
                    case "num_services_crit":
                        hostLiveInfo.setServiceCritical(valueNode.asText());
                        break;
                    default:
                        break;
                }
            }
            hostLiveInfoList.add(hostLiveInfo);
        }
        return hostLiveInfoList;
    }
}
