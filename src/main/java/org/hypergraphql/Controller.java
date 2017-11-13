package org.hypergraphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResultImpl;
import graphql.GraphQL;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import graphql.language.Document;
import graphql.parser.Parser;
import graphql.schema.GraphQLSchema;
import graphql.validation.ValidationError;
import graphql.validation.Validator;
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

//                    System.out.println(
//                            "Path: " + req.pathInfo().toString() +
//                                    " Ip: " + req.ip() +
//                                    " TimeStamp: " + new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(Calendar.getInstance().getTime()));
//                  //  System.out.println(query.toString());


//            Validator validator = new Validator();
//
//            Parser parser = new Parser();
//
//            try {
//                Document document = parser.parseDocument(query);
//                List<ValidationError> validationErrors = validator.validateDocument(schema, document);
//                validationErrors.forEach(err -> System.out.println(err.getMessage()));
//            } catch (Exception e) {
//
//
////            if (validationErrors.size() > 0) {
////                return new ExecutionResultImpl(validationErrors);
//            }

                   try {
                        Map<String, Object> result = service.results(query);

                        JsonNode resultJson = mapper.readTree(new ObjectMapper().writeValueAsString(result));
                        res.type("application/json");

                        return resultJson;
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                        e.fillInStackTrace();
                        res.status(400);
                        return "Error 400: The provided query is either illegal by the GraphQL spec or currently unsupported by HyperGraphQL.";
                    }

        });


        before("/", (req, res) -> {

            res.redirect("https://github.com/semantic-integration/hypergraphql");
            res.status(303);

            System.out.println("redirecting");

        });
    }

}
