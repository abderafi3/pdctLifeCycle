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


    public String fetchHostsLiveInfoFromCheckmk(){
        String apiUrl = checkmkConfig.getApiUrl() + "/view.py?output_format=json_export&view_name=allhosts";
        ResponseEntity<String> response = restClientService.sendGetRequest(apiUrl, String.class);
        return response.getBody();
    }

    public List<HostLiveInfo> convertToHostLiveInfo() throws Exception{
        JsonNode jsonNode = objectMapper.readTree(fetchHostsLiveInfoFromCheckmk());
        Iterator<JsonNode> elements = jsonNode.elements();
        List<String> keys = new ArrayList<>();
        if (elements.hasNext()) {
            for (JsonNode keyNode: elements.next()){
                keys.add(keyNode.asText());
            }
        }

        List<HostLiveInfo> hostLiveInfoList = new ArrayList<>();
        while (elements.hasNext()) {
            JsonNode valuesNode = elements.next();
            HostLiveInfo hostLiveInfo = new HostLiveInfo();
            for (int i = 0; i < keys.size(); i++){
                String key = keys.get(i);
                JsonNode valueNode = valuesNode.get(i);

                switch (key){
                    case "host" -> hostLiveInfo.setHostName(valueNode.asText());
                    case "host_state" -> hostLiveInfo.setHostState(valueNode.asText());
                    case "num_services_ok" -> hostLiveInfo.setServiceOk(valueNode.asText());
                    case "num_services_warn" -> hostLiveInfo.setServiceWarning(valueNode.asText());
                    case "num_services_crit" -> hostLiveInfo.setServiceCritical(valueNode.asText());
                }
            }
            hostLiveInfoList.add(hostLiveInfo);
        }
        return hostLiveInfoList;
    }
}
