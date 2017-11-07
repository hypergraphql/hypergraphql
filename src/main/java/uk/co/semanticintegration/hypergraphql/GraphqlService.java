package uk.co.semanticintegration.hypergraphql;

import com.fasterxml.jackson.databind.JsonNode;
import graphql.*;
import graphql.language.SourceLocation;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by szymon on 01/11/2017.
 */
public class GraphqlService {
    private GraphQL graphQL;
    private Config config;
    static Logger logger = Logger.getLogger(GraphqlService.class);


    public GraphqlService(Config config, GraphQL graphQL) {
        this.graphQL = graphQL;
        this.config = config;
    }

    public Map<String, Object> results(String query) {

        Map<String, Object> result = new HashMap<>();

        Map<String, Object> data = new HashMap<>();
        Map<Object, Object> extensions = new HashMap<>();
        List<GraphQLError> errors = new ArrayList<>();

        ExecutionInput executionInput;
        ExecutionResult qlResult;

        List<String> sparqlQueries;

        if (!query.contains("IntrospectionQuery") && !query.contains("__")) {

            Converter converter = new Converter(config);

            JsonNode jsonQuery = converter.query2json(query);

            sparqlQueries = converter.graphql2sparql(jsonQuery);

            // uncomment this line if you want to include the generated SPARQL queries in the GraphQL response for debugging purposes
            // extensions.put("sparqlQueries", sparqlQueries);

            logger.info("Generated SPARQL queries:");
            logger.info(sparqlQueries.toString());

            SparqlClient client = new SparqlClient(sparqlQueries, config.sparqlEndpointsContext());

            executionInput = ExecutionInput.newExecutionInput()
                    .query(query)
                    .context(client)
                    .build();

            qlResult = graphQL.execute(executionInput);
            try {
                data = converter.jsonLDdata(qlResult.getData(), jsonQuery);
            } catch (IOException e) {
                logger.error(e);
            }

        } else {
            qlResult = graphQL.execute(query);
            data = qlResult.getData();
        }

        errors.addAll(qlResult.getErrors());

        result.put("data", data);
        result.put("errors", errors);
        result.put("extensions", extensions);

        return result;
    }
}
