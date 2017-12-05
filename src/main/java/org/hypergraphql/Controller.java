package org.hypergraphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.services.HGQLService;
import spark.ModelAndView;
import spark.template.velocity.VelocityTemplateEngine;

import static spark.Spark.*;

/**
 * Created by szymon on 05/09/2017.
 */
public class Controller {


    public static void start() {

        HGQLConfig config = HGQLConfig.getInstance();


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
        HGQLService service = new HGQLService();

        // post method for accessing the GraphQL service

        post(config.graphqlConfig().path(), (req, res) -> {

            JsonNode requestObject = mapper.readTree(req.body().toString());

            String query = requestObject.get("query").asText();

            Map<String, Object> result = service.results(query, req.headers("accept"));

            JsonNode resultJson = mapper.readTree(new ObjectMapper().writeValueAsString(result));
            res.type("application/json");

            return resultJson;

        });
    }

}
