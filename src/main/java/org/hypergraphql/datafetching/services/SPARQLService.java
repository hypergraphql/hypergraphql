package org.hypergraphql.datafetching.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.hypergraphql.config.system.ServiceConfig;

public abstract class SPARQLService extends Service {

    protected String graph;

    public String getGraph() {
        return graph;
    }

    public void setParameters(ServiceConfig serviceConfig) {

        this.id = serviceConfig.getId();
        if (serviceConfig.getGraph()!=null)
        this.graph = serviceConfig.getGraph();
        else
            this.graph="";

    }



}
