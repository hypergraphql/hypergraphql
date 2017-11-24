package org.hypergraphql;

import com.fasterxml.jackson.databind.JsonNode;
import org.hypergraphql.config.ServiceConfig;

import java.util.Map;
import java.util.Set;

public class TreeExecutionNode {
    private ServiceConfig service; //service configuration
    private JsonNode query; //GraphQL in a basic Json format
    private String executionId; // unique identifier of this execution node
    private String parentInfo; //URIs of resources that serve as the root for this query
    private Set<TreeExecutionNode> childrenNodes; // succeeding executions

    public ServiceConfig getService() {
        return service;
    }

    public void setService(ServiceConfig service) {
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

    public String getParentInfo() {
        return parentInfo;
    }

    public void setParentInfo(String parentInfo) {
        this.parentInfo = parentInfo;
    }

    public Set<TreeExecutionNode> getChildrenNodes() {
        return childrenNodes;
    }

    public void setChildrenNodes(Set<TreeExecutionNode> childrenNodes) {
        this.childrenNodes = childrenNodes;
    }
}
