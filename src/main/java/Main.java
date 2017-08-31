import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.schema.GraphQLSchema;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spark.Spark.*;

public class Main {
    public static void main(String[] args) {

        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.OFF);

        port(8890);

        System.out.println("GraphQL server started at: http://localhost:8890");


        RDFSchema rdfschema = new RDFSchema();

        System.out.println(rdfschema.GRAPHQL_NAMES.toString());
        System.out.println(rdfschema.NAME_MAP.toString());
        System.out.println(rdfschema.NAME_TO_URI_MAP.toString());

        GraphQL graphQL = GraphQL.newGraphQL(rdfschema.schema).build();

/*
        String q = "\n  query IntrospectionQuery {\n    __schema {\n      queryType { name }\n      mutationType { name }\n      subscriptionType { name }\n      types {\n        ...FullType\n      }\n      directives {\n        name\n        description\n        locations\n        args {\n          ...InputValue\n        }\n      }\n    }\n  }\n\n  fragment FullType on __Type {\n    kind\n    name\n    description\n    fields(includeDeprecated: true) {\n      name\n      description\n      args {\n        ...InputValue\n      }\n      type {\n        ...TypeRef\n      }\n      isDeprecated\n      deprecationReason\n    }\n    inputFields {\n      ...InputValue\n    }\n    interfaces {\n      ...TypeRef\n    }\n    enumValues(includeDeprecated: true) {\n      name\n      description\n      isDeprecated\n      deprecationReason\n    }\n    possibleTypes {\n      ...TypeRef\n    }\n  }\n\n  fragment InputValue on __InputValue {\n    name\n    description\n    type { ...TypeRef }\n    defaultValue\n  }\n\n  fragment TypeRef on __Type {\n    kind\n    name\n    ofType {\n      kind\n      name\n      ofType {\n        kind\n        name\n        ofType {\n          kind\n          name\n          ofType {\n            kind\n            name\n            ofType {\n              kind\n              name\n              ofType {\n                kind\n                name\n                ofType {\n                  kind\n                  name\n                }\n              }\n            }\n          }\n        }\n      }\n    }\n  }\n";
        System.out.println(q);
        Map<String, Object> qa = graphQL.execute(q).getData();
        System.out.println(qa.toString());
*/

        //"{ _graph(type: \"http://www.w3.org/2004/02/skos/core#Concept\", limit: 3) { _id ns4_narrower { _id } } }"


        post("/", (req, res) -> {

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
                extensions.put("json-ld", RDFSchema.graphql2jsonld(data));
            }

            if (data!=null) result.put("data", data); // you have to wrap it into a data json key!
            if (!errors.isEmpty()) result.put("errors", errors);
            if (extensions!=null) result.put("extensions", extensions);

            JsonNode resultJson = mapper.readTree( new ObjectMapper().writeValueAsString(result));
            res.type("application/json");

            return resultJson;

        });


            //String jsonld = RDFSchema.graphql2jsonld(json);
        //    res.type("application/json");
          //  return "{data:"+ json + "}";
        //});





    }
}
