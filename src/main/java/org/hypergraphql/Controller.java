package org.hypergraphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import graphql.GraphQLError;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.services.HGQLQueryService;
import spark.ModelAndView;
import spark.Service;
import spark.template.velocity.VelocityTemplateEngine;

/**
 * Created by szymon on 05/09/2017.
 */
public class Controller {

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


    public void start(HGQLConfig config) {

        System.out.println("HGQL service name: " + config.getName());
        System.out.println("GraphQL server started at: http://localhost:" + config.getGraphqlConfig().port() + config.getGraphqlConfig().graphqlPath());
        System.out.println("GraphiQL UI available at: http://localhost:" + config.getGraphqlConfig().port() + config.getGraphqlConfig().graphiqlPath());

        Service hgqlService = Service.ignite().port(config.getGraphqlConfig().port());


        // get method for accessing the GraphiQL UI

        hgqlService.get(config.getGraphqlConfig().graphiqlPath(), (req, res) -> {

            Map<String, String> model = new HashMap<>();

            model.put("template", String.valueOf(config.getGraphqlConfig().graphqlPath()));

            return new VelocityTemplateEngine().render(
                    new ModelAndView(model, "graphiql.vtl")
            );
        });


        // post method for accessing the GraphQL getSetvice

        hgqlService.post(config.getGraphqlConfig().graphqlPath(), (req, res) -> {
            ObjectMapper mapper = new ObjectMapper();
            HGQLQueryService service = new HGQLQueryService(config);

            JsonNode requestObject = mapper.readTree(req.body().toString());

            String query = requestObject.get("query").asText();

            String acceptType = req.headers("accept");

            String mime = MIME_MAP.containsKey(acceptType) ? MIME_MAP.get(acceptType) : null;
            String contentType = MIME_MAP.containsKey(acceptType) ? acceptType : "application/json";
            Boolean graphQLcompatible = (GRAPHQL_COMPATIBLE_TYPE.containsKey(acceptType)) ? GRAPHQL_COMPATIBLE_TYPE.get(acceptType) : true;

            res.type(contentType);

            Map<String, Object> result = service.results(query, mime);

            List<GraphQLError> errors = (List<GraphQLError>) result.get("errors");
            if (!errors.isEmpty()) {
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

                    JsonNode errorsJson = mapper.readTree(new ObjectMapper().writeValueAsString(errors));
                    resultString = errorsJson.toString();

                }

                return resultString;
            }

        });

        //Return the internal HGQL schema representation as rdf.

        hgqlService.get(config.getGraphqlConfig().graphqlPath() , (req, res) -> {

            String acceptType = req.headers("accept");

            Boolean rdfContentType = (MIME_MAP.containsKey(acceptType) && GRAPHQL_COMPATIBLE_TYPE.containsKey(acceptType) && !GRAPHQL_COMPATIBLE_TYPE.get(acceptType));
            String mime = rdfContentType ? MIME_MAP.get(acceptType) : "RDF/XML";
            String contentType = rdfContentType ? acceptType : "application/rdf+xml";

            res.type(contentType);

            return config.getHgqlSchema().getRdfSchemaOutput(mime);
        });
    }



}
