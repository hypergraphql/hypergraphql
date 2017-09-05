import graphql.GraphQL;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class Main {
    public static void main(String[] args) {

        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.OFF);

        Config config = new Config("properties.json");

         GraphqlWiring wiring = new GraphqlWiring(config);


        System.out.println(wiring.GRAPHQL_NAMES.toString());
        System.out.println(wiring.NAME_TO_URI_MAP.toString());

        GraphQL graphQL = GraphQL.newGraphQL(wiring.schema).build();

        Controller.start(config, graphQL);

/*
        String q = "\n  query IntrospectionQuery {\n    __schema {\n      queryType { name }\n      mutationType { name }\n      subscriptionType { name }\n      types {\n        ...FullType\n      }\n      directives {\n        name\n        description\n        locations\n        args {\n          ...InputValue\n        }\n      }\n    }\n  }\n\n  fragment FullType on __Type {\n    kind\n    name\n    description\n    fields(includeDeprecated: true) {\n      name\n      description\n      args {\n        ...InputValue\n      }\n      type {\n        ...TypeRef\n      }\n      isDeprecated\n      deprecationReason\n    }\n    inputFields {\n      ...InputValue\n    }\n    interfaces {\n      ...TypeRef\n    }\n    enumValues(includeDeprecated: true) {\n      name\n      description\n      isDeprecated\n      deprecationReason\n    }\n    possibleTypes {\n      ...TypeRef\n    }\n  }\n\n  fragment InputValue on __InputValue {\n    name\n    description\n    type { ...TypeRef }\n    defaultValue\n  }\n\n  fragment TypeRef on __Type {\n    kind\n    name\n    ofType {\n      kind\n      name\n      ofType {\n        kind\n        name\n        ofType {\n          kind\n          name\n          ofType {\n            kind\n            name\n            ofType {\n              kind\n              name\n              ofType {\n                kind\n                name\n                ofType {\n                  kind\n                  name\n                }\n              }\n            }\n          }\n        }\n      }\n    }\n  }\n";
        System.out.println(q);
        Map<String, Object> qa = graphQL.execute(q).getData();
        System.out.println(qa.toString());
*/

        //"{ _graph(type: \"http://www.w3.org/2004/02/skos/core#Concept\", limit: 3) { _id ns4_narrower { _id } } }"





            //String jsonld = GraphqlWiring.graphql2jsonld(json);
        //    res.type("application/json");
          //  return "{data:"+ json + "}";
        //});




    }
}
