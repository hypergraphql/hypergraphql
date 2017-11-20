package org.hypergraphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;

import java.util.HashMap;
import java.util.Map;

import graphql.schema.GraphQLSchema;
import spark.ModelAndView;
import spark.template.velocity.VelocityTemplateEngine;

import static spark.Spark.*;

/**
 * Created by szymon on 05/09/2017.
 */
public class Controller {


    public static void start(Config config) {


        port(config.graphqlConfig().port());


        // get method for accessing the GraphiQL UI

        get(config.graphqlConfig().graphiql(), (req, res) -> {

            Map<String, String> model = new HashMap<>();

            model.put("template", String.valueOf(config.graphqlConfig().path()));

            return new VelocityTemplateEngine().render(
                    new ModelAndView(model, "graphiql.vtl")
            );
        });


        ObjectMapper mapper = new ObjectMapper();
        GraphqlService service = new GraphqlService(config);

        // post method for accessing the GraphQL service

        post(config.graphqlConfig().path(), (req, res) -> {

            JsonNode requestObject = mapper.readTree(req.body().toString());

            String query = requestObject.get("query").asText();

            Map<String, Object> result = service.results(query);

            JsonNode resultJson = mapper.readTree(new ObjectMapper().writeValueAsString(result));
            res.type("application/json");

            return resultJson;

        });
    }

}
