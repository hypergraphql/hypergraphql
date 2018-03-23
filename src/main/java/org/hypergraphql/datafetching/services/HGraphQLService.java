package org.hypergraphql.datafetching.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.hypergraphql.config.system.ServiceConfig;
import org.hypergraphql.datafetching.TreeExecutionResult;
import org.hypergraphql.datamodel.HGQLSchema;
import org.hypergraphql.query.converters.HGraphQLConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;

public class HGraphQLService extends Service {

    private String url;
    private String user;
    private String password;

    private final static Logger LOGGER = LoggerFactory.getLogger(HGraphQLService.class);

    @Override
    public TreeExecutionResult executeQuery(JsonNode query, Set<String> input, Set<String> markers , String rootType, HGQLSchema schema) {

        Model model;
        Map<String, Set<String>> resultSet;
        String graphQlQuery = new HGraphQLConverter(schema).convertToHGraphQL(query, input, rootType);
        model = getModelFromRemote(graphQlQuery);

        resultSet = getResultset(model, query, input, markers, schema);

        TreeExecutionResult treeExecutionResult = new TreeExecutionResult();
        treeExecutionResult.setResultSet(resultSet);
        treeExecutionResult.setModel(model);

        return treeExecutionResult;
    }

    Model getModelFromRemote(String graphQlQuery) {

        ObjectMapper mapper = new ObjectMapper();

        ObjectNode bodyParam = mapper.createObjectNode();

        bodyParam.put("query", graphQlQuery);

        Model model = ModelFactory.createDefaultModel();

        LOGGER.info("\n" + url);
        LOGGER.info("\n" + graphQlQuery);

        try {

            HttpResponse<InputStream> response = Unirest.post(url)
                    .header("Accept", "application/rdf+xml")
                    .body(bodyParam.toString())
                    .asBinary();

            model.read(response.getBody(), "RDF/XML");

        } catch (UnirestException e) {
            e.printStackTrace();
        }

        return model;
    }

    @Override
    public void setParameters(ServiceConfig serviceConfig) {

        this.id = serviceConfig.getId();
        this.url = serviceConfig.getUrl();
        this.user = serviceConfig.getUser();
        this.password = serviceConfig.getPassword();

    }
}

