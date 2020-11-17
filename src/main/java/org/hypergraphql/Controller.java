package org.hypergraphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQLError;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.services.HGQLQueryService;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Service;
import spark.template.velocity.VelocityTemplateEngine;

import static org.hypergraphql.util.ControllerConstants.ORIGIN_HEADER;
import static org.hypergraphql.util.ControllerConstants.WILDCARD;
import static spark.Spark.before;

/**
 * Created by szymon on 05/09/2017.
 *
 * This is the primary &quot;Controller&quot; used by the application.
 * The handler methods are in the get() and post() lambdas
 */
@Slf4j
public class Controller {

    private static final String DEFAULT_MIME_TYPE = "RDF/XML";
    private static final String DEFAULT_ACCEPT_TYPE = "application/rdf+xml";

    private static final Map<String, String> APPLICATION_MIME_MAP = Map.of(
        "application/json+rdf+xml", "RDF/XML",
        "application/json+turtle", "TTL",
        "application/json+ntriples", "N-TRIPLES",
        "application/json+n3", "N3",
        "application/rdf+xml", "RDF/XML",
        "application/turtle", "TTL",
        "application/ntriples", "N-TRIPLES",
        "application/n3", "N3"
    );

    private static final Map<String, String> TEXT_MIME_MAP = Map.of(
        "text/turtle", "TTL",
        "text/ntriples", "N-TRIPLES",
        "text/n3", "N3"
    );

    private static final Map<String, String> MIME_MAP = new HashMap<>();

    {
        MIME_MAP.putAll(APPLICATION_MIME_MAP);
        MIME_MAP.putAll(TEXT_MIME_MAP);
    }

    private static final Map<String, Boolean> APPLICATION_GQL_TYPES = Map.of(
        "application/json+rdf+xml", true,
        "application/json+turtle", true,
        "application/json+ntriples", true,
        "application/json+n3", true,
        "application/rdf+xml", false,
        "application/turtle", false,
        "application/ntriples", false,
        "application/n3", false
    );

    private static final Map<String, Boolean> TEXT_GQL_TYPES = Map.of(
            "text/turtle", false,
            "text/ntriples", false,
            "text/n3", false
    );

    private static final Map<String, Boolean> GRAPHQL_COMPATIBLE_TYPE = new HashMap<>();

    {
        GRAPHQL_COMPATIBLE_TYPE.putAll(APPLICATION_GQL_TYPES);
        GRAPHQL_COMPATIBLE_TYPE.putAll(TEXT_GQL_TYPES);
    }

    private static final int BAD_REQUEST_CODE = 400;

    private Service hgqlService;

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
            setResponseHeaders(req, res);
            return "";
        });

        // get method for accessing the GraphiQL UI

        hgqlService.get(config.getGraphqlConfig().graphiQLPath(), (req, res) -> {

            Map<String, String> model = new HashMap<>();

            model.put("template", String.valueOf(config.getGraphqlConfig().graphQLPath()));

            setResponseHeaders(req, res);

            return new VelocityTemplateEngine().render(
                    new ModelAndView(model, "graphiql.vtl")
            );
        });

        // post method for accessing the GraphQL getService
        hgqlService.post(config.getGraphqlConfig().graphQLPath(), (req, res) -> {

            val service = new HGQLQueryService(config);

            val query = consumeRequest(req);
            val acceptType = req.headers("accept");

            val mime = MIME_MAP.getOrDefault(acceptType, null);
            val contentType = MIME_MAP.getOrDefault(acceptType, "application/json");
            val graphQLCompatible = GRAPHQL_COMPATIBLE_TYPE.getOrDefault(acceptType, true);

            res.type(contentType);

            final Map<String, Object> result = service.results(query, mime);

            final List<GraphQLError> errors = (List<GraphQLError>) result.get("errors");
            if (!errors.isEmpty()) {
                res.status(BAD_REQUEST_CODE);
            }

            setResponseHeaders(req, res);

            val mapper = new ObjectMapper();
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

        // Return the internal HGQL schema representation as rdf.

        hgqlService.get(config.getGraphqlConfig().graphQLPath(), (req, res) -> {

            val acceptType = req.headers("accept"); // TODO

            val isRdfContentType =
                    MIME_MAP.containsKey(acceptType)
                            && GRAPHQL_COMPATIBLE_TYPE.containsKey(acceptType)
                            && !GRAPHQL_COMPATIBLE_TYPE.get(acceptType);
            final String mime = isRdfContentType ? MIME_MAP.get(acceptType) : DEFAULT_MIME_TYPE;
            final String contentType = isRdfContentType ? acceptType : DEFAULT_ACCEPT_TYPE;

            res.type(contentType);

            setResponseHeaders(req, res);

            return config.getHgqlSchema().getRdfSchemaOutput(mime);
        });
    }

    private String consumeRequest(final Request request) throws IOException {

        if (request.contentType().equalsIgnoreCase("application-x/graphql")) { // TODO
            return consumeGraphQLBody(request.body());
        } else {
            return consumeJSONBody(request.body());
        }
    }

    private String consumeJSONBody(final String body) throws IOException {

        val mapper = new ObjectMapper();
        val requestObject = mapper.readTree(body);
        if (requestObject.get("query") == null) {
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

        if (hgqlService != null) {
            log.info("Attempting to shut down service at http://localhost:" + hgqlService.port() + "...");
            hgqlService.stop();
            log.info("Shut down server");
        }
    }

    private void setResponseHeaders(final Request request, final Response response) {

        final List<String> headersList = Arrays.asList(
                ORIGIN_HEADER,
                "X-Requested-With",
                "Content-Type",
                "Accept",
                "authorization",
                "x-auth-token"
        );

        val origin = Objects.requireNonNullElse(request.headers(ORIGIN_HEADER), WILDCARD);
        response.header("Access-Control-Allow-Origin", origin); // TODO
        if (!WILDCARD.equals(origin)) {
            response.header("Vary", ORIGIN_HEADER);
        }
        response.header("Access-Control-Allow-Headers", StringUtils.join(headersList, ",")); // TODO

        response.header("Access-Control-Allow-Credentials", "true"); // TODO
    }
}
