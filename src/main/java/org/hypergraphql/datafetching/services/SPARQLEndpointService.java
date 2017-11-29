package org.hypergraphql.datafetching.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.hypergraphql.datafetching.TreeExecutionResult;

import java.util.Set;

public class SPARQLEndpointService extends SPARQLService {
    public SPARQLEndpointService(String type, String id, String url, String user, String graph, String password) {
        super(type, id, url, user, graph, password);
    }

    @Override
    public TreeExecutionResult executeQuery(JsonNode query, Set<String> input) {
        return null;
    }
}
