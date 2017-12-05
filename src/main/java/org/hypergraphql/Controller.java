package org.hypergraphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import graphql.GraphQLError;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.services.HGQLService;
import spark.ModelAndView;
import spark.template.velocity.VelocityTemplateEngine;

import static spark.Spark.*;

/**
 * Created by szymon on 05/09/2017.
 */
public class Controller {

    private static HGQLConfig config = HGQLConfig.getInstance();

    private static final Map<String, String> MIME_MAP = new HashMap<String, String>() {{
        put("application/json+rdf+xml", "RDF/XML");
        put("application/json+turtle", "TTL");
        put("application/json+ntriples", "N-TRIPLES");
        put("application/json+n3", "N3");
        put("application/rdf+xml", "RDF/XML");
        put("application/turtle", "TTL");
        put("application/ntriples", "N-TRIPLES");
        put("application/n3", "N3");
        put("text/turtle", "TTL");
        put("text/ntriples", "N-TRIPLES");
        put("text/n3", "N3");
    }};

    private static final Map<String, Boolean> GRAPHQL_COMPATIBLE_TYPE = new HashMap<String, Boolean>() {{
        put("application/json+rdf+xml", true);
        put("application/json+turtle", true);
        put("application/json+ntriples", true);
        put("application/json+n3", true);
        put("application/rdf+xml", false);
        put("application/turtle", false);
        put("application/ntriples", false);
        put("application/n3", false);
        put("text/turtle", false);
        put("text/ntriples", false);
        put("text/n3", false);
    }};


    public static void start() {


        port(config.graphqlConfig().port());


        // get method for accessing the GraphiQL UI

        get(config.graphqlConfig().graphiql(), (req, res) -> {

            Map<String, String> model = new HashMap<>();

            model.put("template", String.valueOf(config.graphqlConfig().path()));

            return new VelocityTemplateEngine().render(
                    new ModelAndView(model, "graphiql.vtl")
            );
        });


        get("/context.jsonld", (req, res) -> {

            Map<String, String> model = new HashMap<>();

            return new VelocityTemplateEngine().render(
                    new ModelAndView(model, "context.jsonld")
            );
        });


        ObjectMapper mapper = new ObjectMapper();
        HGQLService service = new HGQLService();


        // post method for accessing the GraphQL service

        post(config.graphqlConfig().path(), (req, res) -> {

            JsonNode requestObject = mapper.readTree(req.body().toString());

            String query = requestObject.get("query").asText();

            String acceptType = req.headers("accept");

            String mime = MIME_MAP.containsKey(acceptType) ? MIME_MAP.get(acceptType) : null;
            String contentType = MIME_MAP.containsKey(acceptType) ? acceptType : "application/json";
            Boolean graphQLcompatible = (GRAPHQL_COMPATIBLE_TYPE.containsKey(acceptType)) ? GRAPHQL_COMPATIBLE_TYPE.get(acceptType) : true;

            res.type(contentType);

            Map<String, Object> result = service.results(query, mime);


            if (!((List<GraphQLError>) result.get("errors")).isEmpty()) {
                res.status(400);
            }

            if (graphQLcompatible) {

                JsonNode resultJson = mapper.readTree(new ObjectMapper().writeValueAsString(result));
                return resultJson;

            } else {
                String resultString;

                if (result.containsKey("data")) {
                    resultString = result.get("data").toString();
                } else {

                    JsonNode errors = mapper.readTree(new ObjectMapper().writeValueAsString(result.get("errors")));
                    resultString = errors.toString();

                }

                return resultString;
            }

        });
    }

}
