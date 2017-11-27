package org.hypergraphql;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.jena.rdf.model.Model;
import org.hypergraphql.config.Service;

import java.util.Map;
import java.util.Set;

public class TreeExecutionNode {
    private Service service; //service configuration
    private JsonNode query; //GraphQL in a basic Json format
    private String executionId; // unique identifier of this execution node
    private Map<String, Set<TreeExecutionNode>> childrenNodes; // succeeding executions

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public JsonNode getQuery() {
        return query;
    }

    public void setQuery(JsonNode query) {
        this.query = query;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }
    

    public Model generateModel() {
        //todo
        return null;
    }
}
