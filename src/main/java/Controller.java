import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import org.apache.jena.rdf.model.Model;
import spark.ModelAndView;
import spark.template.velocity.VelocityTemplateEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

            ExecutionResult qlResult;

            if (!query.contains("IntrospectionQuery")) {
                Set<String> sparqlQueries = converter.graphql2sparql(query);

                SparqlClient client = new SparqlClient(config);
                Model model = client.getRdfModel(sparqlQueries);

              //  model.write(System.out);

                ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                        .query(query)
                        .context(model)
                        .build();

                qlResult = graphQL.execute(executionInput);

            } else qlResult = graphQL.execute(query);

            Map<String, Object> data = qlResult.getData();
            List<GraphQLError> errors = qlResult.getErrors();
            Map<String, Object> result = new HashMap<>();
            Map<Object, Object> extensions = qlResult.getExtensions();

            if (extensions==null) extensions = new HashMap<>();
            if (data!=null&&!data.containsKey("__schema")) {

                extensions.put("json-ld", converter.graphql2jsonld(data));
            }

            if (data!=null) result.put("data", data);

            if (!errors.isEmpty()) result.put("errors", errors);
            if (extensions!=null) result.put("extensions", extensions);

            JsonNode resultJson = mapper.readTree( new ObjectMapper().writeValueAsString(result));
            res.type("application/json");

            return resultJson;

        });
    }


}
