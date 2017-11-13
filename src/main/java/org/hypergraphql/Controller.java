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


    public static void start(Config config, GraphQL graphQL, GraphQLSchema schema) {


        port(config.graphql().port());


        // get method for accessing the GraphiQL UI

        get(config.graphql().graphiql(), (req, res) -> {

            Map<String, String> model = new HashMap<>();

            model.put("template", String.valueOf(config.graphql().path()));

            return new VelocityTemplateEngine().render(
                    new ModelAndView(model, "graphiql.vtl")
            );
        });



        ObjectMapper mapper = new ObjectMapper();
        GraphqlService service  = new GraphqlService(config, graphQL);

        // post method for accessing the GraphQL service

        post(config.graphql().path(), (req, res) -> {

            JsonNode requestObject = mapper.readTree(req.body().toString());

            String query = requestObject.get("query").asText();

           try {
                Map<String, Object> result = service.results(query, schema);

                JsonNode resultJson = mapper.readTree(new ObjectMapper().writeValueAsString(result));
                res.type("application/json");

                return resultJson;
            } catch (Exception e) {
                res.status(400);
                return "Error 400: The requested query contains illegal syntax or is currently unsupported by HyperGraphQL.";
            }

        });


        before("/", (req, res) -> {

            res.redirect("https://github.com/semantic-integration/hypergraphql");
            res.status(303);

            System.out.println("redirecting");

        });
    }

}
