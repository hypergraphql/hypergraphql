package org.hypergraphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import graphql.GraphQLError;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.services.HGQLQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Service;
import spark.template.velocity.VelocityTemplateEngine;

import static spark.Spark.before;

/**
 * Created by szymon on 05/09/2017.
 *
 * This is the primary &quot;Controller&quot; used by the application.
 * The handler methods are in the get() and post() lambdas
 */
public class Controller {

    private final static Logger LOGGER = LoggerFactory.getLogger(Controller.class);

    private Service hgqlService;

    private static final String DEFAULT_MIME_TYPE = "RDF/XML";
    private static final String DEFAULT_ACCEPT_TYPE = "application/rdf+xml";

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
        System.out.println("GraphQL server started at: http://localhost:" + config.getGraphqlConfig().port() + config.getGraphqlConfig().graphQLPath());
        System.out.println("GraphiQL UI available at: http://localhost:" + config.getGraphqlConfig().port() + config.getGraphqlConfig().graphiQLPath());

        hgqlService = Service.ignite().port(config.getGraphqlConfig().port());

        // CORS
        before((request, response) -> {
            response.header("Access-Control-Allow-Methods", "OPTIONS,GET,POST");
            response.header("Content-Type", "");
        });

        hgqlService.options("/*", (req, res) -> {
            setResponseHeaders(res);
            return "";
        });

        // get method for accessing the GraphiQL UI

        hgqlService.get(config.getGraphqlConfig().graphiQLPath(), (req, res) -> {

            Map<String, String> model = new HashMap<>();

            model.put("template", String.valueOf(config.getGraphqlConfig().graphQLPath()));

            setResponseHeaders(res);

            return new VelocityTemplateEngine().render(
                    new ModelAndView(model, "graphiql.vtl")
            );
        });

        // post method for accessing the GraphQL getService
        hgqlService.post(config.getGraphqlConfig().graphQLPath(), (req, res) -> {

            HGQLQueryService service = new HGQLQueryService(config);

            final String query = consumeRequest(req);
            String acceptType = req.headers("accept");

            String mime = MIME_MAP.getOrDefault(acceptType, null);
            String contentType = MIME_MAP.containsKey(acceptType) ? acceptType : "application/json";
            Boolean graphQLCompatible = GRAPHQL_COMPATIBLE_TYPE.getOrDefault(acceptType, true);

            res.type(contentType);

            Map<String, Object> result = service.results(query, mime);

            List<GraphQLError> errors = (List<GraphQLError>) result.get("errors");
            if (!errors.isEmpty()) {
                res.status(400);
            }

            setResponseHeaders(res);

            ObjectMapper mapper = new ObjectMapper();
            if (graphQLCompatible) {
                return mapper.readTree(new ObjectMapper().writeValueAsString(result));
            } else {
                if (result.containsKey("data")) {
                    return result.get("data").toString();
                } else {
                    JsonNode errorsJson = mapper.readTree(new ObjectMapper().writeValueAsString(errors));
                    return errorsJson.toString();
                }
            }
        });

        //Return the internal HGQL schema representation as rdf.

        hgqlService.get(config.getGraphqlConfig().graphQLPath() , (req, res) -> {

            String acceptType = req.headers("accept");

            Boolean isRdfContentType =
                    (MIME_MAP.containsKey(acceptType)
                            && GRAPHQL_COMPATIBLE_TYPE.containsKey(acceptType)
                            && !GRAPHQL_COMPATIBLE_TYPE.get(acceptType));
            String mime = isRdfContentType ? MIME_MAP.get(acceptType) : DEFAULT_MIME_TYPE;
            String contentType = isRdfContentType ? acceptType : DEFAULT_ACCEPT_TYPE;

            res.type(contentType);

            setResponseHeaders(res);

            return config.getHgqlSchema().getRdfSchemaOutput(mime);
        });
    }

    private String consumeRequest(final Request request) throws IOException {

        if(request.contentType().equalsIgnoreCase("application-x/graphql")) {
            return consumeGraphQLBody(request.body());
        } else {
            return consumeJSONBody(request.body());
        }
    }

    private String consumeJSONBody(final String body) throws IOException {

        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode requestObject = mapper.readTree(body);
        if(requestObject.get("query") == null) {
            throw new IllegalArgumentException(
                    "Body appears to be JSON but does not contain required 'query' attribute: " + body
            );
        }
        return requestObject.get("query").asText();
    }

    private String consumeGraphQLBody(final String body) {

        return body;
    }

    public void stop() {

        if(hgqlService != null) {
            LOGGER.info("Attempting to shut down service at http://localhost:" + hgqlService.port() + "...");
            hgqlService.stop();
            LOGGER.info("Shut down server");
        }
    }

    private void setResponseHeaders(final Response response) {

        response.header("Access-Control-Allow-Origin", "*");
        response.header("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, authorization, x-auth-token");
    }
}
