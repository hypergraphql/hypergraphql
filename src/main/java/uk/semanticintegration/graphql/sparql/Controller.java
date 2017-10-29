package uk.semanticintegration.graphql.sparql;

import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import spark.ModelAndView;
import spark.template.velocity.VelocityTemplateEngine;

/**
 * Created by szymon on 05/09/2017.
 */
public class Controller {


    public static void start(Config config, GraphQL graphQL) {

        port(config.graphql().port());

        System.out.println("GraphQL server started at: http://localhost:" + config.graphql().port() + config.graphql().path());
        System.out.println("GraphiQL UI available at: http://localhost:" + config.graphql().port() + config.graphql().graphiql());

        get(config.graphql().graphiql(), (req, res) -> {
            Map<String, String> model = new HashMap<>();
            model.put("template", String.valueOf(config.graphql().path()));
            return new VelocityTemplateEngine().render(
                    new ModelAndView(model, "graphiql.vtl")
            );
        });

        post(config.graphql().path(), (req, res) -> {

            Converter converter = new Converter(config);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode requestObject = mapper.readTree(req.body().toString());

            String query = requestObject.get("query").asText();

            Map<String, Object> result = new HashMap<>();
            Map<String, Object> data = new HashMap<>();
            List<GraphQLError> errors = new ArrayList<>();
            Map<Object, Object> extensions = new HashMap<>();

            ExecutionInput executionInput;
            ExecutionResult qlResult;

            List<String> sparqlQueries;

            if (!query.contains("IntrospectionQuery")&&!query.contains("__")) {

                sparqlQueries = converter.graphql2sparql(query);

                SparqlClient client = new SparqlClient(sparqlQueries, config.sparqlEndpointsContext());

                executionInput = ExecutionInput.newExecutionInput()
                        .query(query)
                        .context(client)
                        .build();

                qlResult = graphQL.execute(executionInput);
                data = converter.jsonLDdata(query, qlResult.getData());

            } else {
                qlResult = graphQL.execute(query);
                data = qlResult.getData();
   }

            errors.addAll(qlResult.getErrors());

            result.put("data", data);
            result.put("errors", errors);
            result.put("extensions", extensions);

            JsonNode resultJson = mapper.readTree(new ObjectMapper().writeValueAsString(result));
            res.type("application/json");

            return resultJson;

        });
    }

}
