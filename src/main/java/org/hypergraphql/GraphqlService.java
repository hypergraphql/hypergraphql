package org.hypergraphql;

import com.fasterxml.jackson.databind.JsonNode;
import graphql.*;
import org.apache.log4j.Logger;
import org.hypergraphql.config.HGQLConfig;
import org.hypergraphql.config.TreeExecutionFactory;
import org.hypergraphql.datamodel.ModelContainer;
import org.hypergraphql.datamodel.StoredModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by szymon on 01/11/2017.
 */
public class GraphqlService {
    private HGQLConfig config;
    static Logger logger = Logger.getLogger(GraphqlService.class);

    public GraphqlService(HGQLConfig config) {
        this.config = config;
    }


    public Map<String, Object> results(String query) {

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        Map<Object, Object> extensions = new HashMap<>();
        List<GraphQLError> errors = new ArrayList<>();

        result.put("data", data);
        result.put("errors", errors);
        result.put("extensions", extensions);

        ExecutionInput executionInput;
        ExecutionResult qlResult;

        List<Map<String, String>> sparqlQueries;

        Converter converter = new Converter(config);
        Map<String, Object> preprocessedQuery = null;
        try {
            preprocessedQuery = converter.query2json(query);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<GraphQLError> validationErrors = (List<GraphQLError>) preprocessedQuery.get("errors");
        errors.addAll(validationErrors);

        if (validationErrors.size() > 0) {

            //  result.put("errors", errors);

            return result;

        }

        if (!query.contains("IntrospectionQuery") && !query.contains("__")) {

            JsonNode jsonQuery = converter.includeContextInQuery((JsonNode) preprocessedQuery.get("query"));

            sparqlQueries = converter.graphql2sparql(jsonQuery);

            // uncomment this lines if you want to include the generated SPARQL queries in the GraphQL response for debugging purposes
            // extensions.put("sparqlQueries", sparqlQueries);

            logger.info("Generated SPARQL queries:");
            logger.info(sparqlQueries.toString());



            TreeExecutionNode queryExecutionTree = new TreeExecutionFactory().getExecutionTree(jsonQuery);

            queryExecutionTree.generateTreeModel(null);

            //todo here wait for threads to finis

            ModelContainer client = new ModelContainer(StoredModel.getInstance());


            executionInput = ExecutionInput.newExecutionInput()
                    .query(query)
                    .context(client)
                    .build();

            qlResult = config.graphql().execute(executionInput);

            data.putAll(qlResult.getData());
            data.put("@context", preprocessedQuery.get("context"));
        } else {
            qlResult = config.graphql().execute(query);
            data.putAll(qlResult.getData());
        }

        errors.addAll(qlResult.getErrors());

        return result;
    }
}
