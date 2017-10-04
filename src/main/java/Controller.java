import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.*;
import spark.ModelAndView;
import spark.template.velocity.VelocityTemplateEngine;

import java.util.*;

import static spark.Spark.*;

/**
 * Created by szymon on 05/09/2017.
 */
public class Controller {

    static Converter converter;


    public static void start(Config config, GraphQL graphQL) {

        converter = new Converter(config);

        port(config.graphql.port);

        System.out.println("GraphQL server started at: "  + config.graphql.port);

        get(config.graphql.graphiql, (req, res) -> {
            Map<String, String> model = new HashMap<>();
            String portScript = config.graphql.port.toString();
            model.put("template", portScript);
            return new VelocityTemplateEngine().render(
                    new ModelAndView(model, "graphiql.vtl")
            ); });

        post(config.graphql.path, (req, res) -> {

            ObjectMapper mapper = new ObjectMapper();
            JsonNode requestObject = mapper.readTree(req.body().toString());

            String query = requestObject.get("query").asText();

            Map<String, Object> result = new HashMap<>();
            Map<String, Object> data = new HashMap<>();
            List<GraphQLError> errors = new ArrayList<>();
            Map<Object, Object> extensions = new HashMap<>();
            result.put("data", data);
            result.put("errors", errors);
            result.put("extensions", extensions);

            ExecutionInput executionInput;
            ExecutionResult qlResult;

            Set<String> sparqlQueries;

            if (!query.contains("IntrospectionQuery")) {

                sparqlQueries = converter.graphql2sparql(query);

                SparqlClient client = new SparqlClient(sparqlQueries);

                executionInput = ExecutionInput.newExecutionInput()
                        .query(query)
                        .context(client)
                        .build();

                qlResult = graphQL.execute(executionInput);

                extensions.put("json-ld", converter.graphql2jsonld(qlResult.getData()));
                extensions.put("sparql-queries", sparqlQueries);

            } else qlResult = graphQL.execute(query);

            data.putAll(qlResult.getData());
            errors.addAll(qlResult.getErrors());

            JsonNode resultJson = mapper.readTree( new ObjectMapper().writeValueAsString(result));
            res.type("application/json");

            return resultJson;

        });
    }


}
