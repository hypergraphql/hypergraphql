package org.hypergraphql.datafetching.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.hypergraphql.datafetching.TreeExecutionResult;

import java.util.Set;

public abstract class SPARQLService extends Service {

    protected String graph;

    public String getGraph() {
        return graph;
    }

    public void setGraph(String graph) {
        this.graph = graph;
    }


    public  void setParameters(JsonNode jsonnode) {

        this.graph = jsonnode.get("graph").asText();
    }


}
