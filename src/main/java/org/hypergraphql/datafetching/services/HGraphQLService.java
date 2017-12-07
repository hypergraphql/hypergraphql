package org.hypergraphql.datafetching.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.org.apache.xpath.internal.operations.Mod;
import org.apache.commons.lang.UnhandledException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.*;
import org.hypergraphql.config.schema.FieldConfig;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.datafetching.TreeExecutionResult;
import org.hypergraphql.datamodel.QueryNode;
import org.hypergraphql.query.converters.HGraphQLConverter;

import javax.jws.WebParam;
import java.util.*;

public class HGraphQLService extends Service {
    private String url;
    private String user;
    private String password;


    @Override
    public TreeExecutionResult executeQuery(JsonNode query, Set<String> input, Set<String> markers) {

        Model model;
        Map<String, Set<String>> resultSet;
        JsonNode graphQlQuery = new HGraphQLConverter().convertToHGraphQL(query, input);
        model = getModelFromRemote(graphQlQuery);

        resultSet = getResultset(model, query, input, markers);

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
    public void setParameters(JsonNode jsonnode) {

        this.id = jsonnode.get("@id").asText();
        this.url = jsonnode.get("url").asText();
        this.user = jsonnode.get("user").asText();
        this.password = jsonnode.get("password").asText();

    }
}

