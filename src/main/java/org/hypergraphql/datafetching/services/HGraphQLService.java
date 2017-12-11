package org.hypergraphql.datafetching.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.jena.rdf.model.*;
import org.hypergraphql.config.system.ServiceConfig;
import org.hypergraphql.datafetching.TreeExecutionResult;
import org.hypergraphql.datamodel.HGQLSchema;
import org.hypergraphql.query.converters.HGraphQLConverter;

import java.util.*;

public class HGraphQLService extends Service {
    private String url;
    private String user;
    private String password;


    @Override
    public TreeExecutionResult executeQuery(JsonNode query, Set<String> input, Set<String> markers , HGQLSchema schema) {

        Model model;
        Map<String, Set<String>> resultSet;
        JsonNode graphQlQuery = new HGraphQLConverter().convertToHGraphQL(query, input);
        model = getModelFromRemote(graphQlQuery);

        resultSet = getResultset(model, query, input, markers, schema);

        TreeExecutionResult treeExecutionResult = new TreeExecutionResult();
        treeExecutionResult.setResultSet(resultSet);
        treeExecutionResult.setModel(model);

        return treeExecutionResult;
    }


    private Model getModelFromRemote(JsonNode graphQlQuery) {

        //todo
        return null;
    }

    @Override
    public void setParameters(ServiceConfig serviceConfig) {

        this.id = serviceConfig.getId();
        this.url = serviceConfig.getUrl();
        this.user = serviceConfig.getUser();
        this.password = serviceConfig.getPassword();

    }
}

