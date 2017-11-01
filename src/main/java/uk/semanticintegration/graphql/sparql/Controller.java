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

        GraphqlService service  = new GraphqlService(config, graphQL);


        port(config.graphql().port());


        // get method for accessing the GraphiQL UI

        get(config.graphql().graphiql(), (req, res) -> {

            Map<String, String> model = new HashMap<>();

            model.put("template", String.valueOf(config.graphql().path()));

            return new VelocityTemplateEngine().render(
                    new ModelAndView(model, "graphiql.vtl")
            );
        });


        // post method for accessing the GraphQL service

        post(config.graphql().path(), (req, res) -> {

            ObjectMapper mapper = new ObjectMapper();
            JsonNode requestObject = mapper.readTree(req.body().toString());

            String query = requestObject.get("query").asText();

            Map<String, Object> result = service.results(query);

            JsonNode resultJson = mapper.readTree(new ObjectMapper().writeValueAsString(result));
            res.type("application/json");

            return resultJson;

        });
    }

}
