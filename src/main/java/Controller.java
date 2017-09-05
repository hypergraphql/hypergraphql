import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spark.Spark.externalStaticFileLocation;
import static spark.Spark.port;
import static spark.Spark.post;

/**
 * Created by szymon on 05/09/2017.
 */
public class Controller {


    public static void start(Config config, GraphQL graphQL) {

        port(config.graphql.port);

        System.out.println("GraphQL server started at: "  + config.graphql.port);

        //this will open the path to serving the graphiql IDE
        String webdir = System.getProperty("user.dir");
        externalStaticFileLocation(webdir);

        post(config.graphql.path, (req, res) -> {

            ObjectMapper mapper = new ObjectMapper();
            JsonNode requestObject = mapper.readTree(req.body().toString());

            String query = requestObject.get("query").asText();

            Map<String, Object> result = new HashMap<>();

            ExecutionResult qlResult = graphQL.execute(query);

            Map<String, Object> data = qlResult.getData();
            List<GraphQLError> errors = qlResult.getErrors();
            Map<Object, Object> extensions = qlResult.getExtensions();

            if (extensions==null) extensions = new HashMap<>();
            if (data!=null && data.containsKey("_graph")) {
                Graphql2Jsonld transformer = new Graphql2Jsonld(config);
                extensions.put("json-ld", transformer.graphql2jsonld(data));
            }

            if (data!=null) result.put("data", data); // you have to wrap it into a data json key!
            if (!errors.isEmpty()) result.put("errors", errors);
            if (extensions!=null) result.put("extensions", extensions);

            JsonNode resultJson = mapper.readTree( new ObjectMapper().writeValueAsString(result));
            res.type("application/json");

            return resultJson;

        });
    }


}
