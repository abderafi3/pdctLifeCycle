package com.checkmk.pdctLifeCycle.model;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;


public class HostDeserializer extends JsonDeserializer<Host> {


    @Override
    public Host deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        JsonNode jsonNode = jsonParser.getCodec().readTree(jsonParser);
        Host host = new Host();

        host.setId(jsonNode.get("id").asText());
        host.setHostName(jsonNode.get("id").asText());
        JsonNode extensionNode = jsonNode.get("extensions");
        JsonNode attributesNode = extensionNode.get("attributes");
        host.setIpAddress(attributesNode.get("ipaddress").asText());
        JsonNode metaDataNode = attributesNode.get("meta_data");

        String creationDate = metaDataNode.get("created_at").asText();

        host.setCreationDate(formatDateTime(creationDate));
return host;
    }


    private String formatDateTime(String isoDateTime) {
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(isoDateTime);
        return zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
