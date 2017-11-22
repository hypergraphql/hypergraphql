package org.hypergraphql.config;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public class PredicateConfig {
    private String id;
    private ServiceConfig service;

    public PredicateConfig(JsonNode predicateJson, Map<String, ServiceConfig> services) {

        if (predicateJson.has("@id")) this.id = predicateJson.get("@id").toString();
        if (predicateJson.has("service")) this.service = services.get(predicateJson.get("service").asText());

    }

    public String id() { return this.id; }
    public ServiceConfig service() { return this.service; }
}
