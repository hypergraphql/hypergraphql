package org.hypergraphql.datafetching.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.hypergraphql.config.system.ServiceConfig;
import org.hypergraphql.datafetching.TreeExecutionResult;

import java.util.Set;

public abstract class Service {

    protected String type;
    protected String id;


    protected Class SPARQLEndpointService ;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }



    public abstract TreeExecutionResult executeQuery(JsonNode query, Set<String> input, String rootType, Set<String> strings);

    public abstract void setParameters(ServiceConfig serviceConfig);



}