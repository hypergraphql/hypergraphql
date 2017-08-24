import graphql.GraphQL;

import java.util.Map;

public class Main {
    public static void main(String[] args) {

        RDFSchema rdfschema = new RDFSchema();

        System.out.println(rdfschema.GRAPHQL_NAMES.toString());

        GraphQL graphQL = GraphQL.newGraphQL(rdfschema.schema).build();

        Map<String, Object> queryAnswer = graphQL.execute("{ _graph(type: \"http://www.w3.org/2004/02/skos/core#Concept\", limit: 3) { _id ns4_narrower { _id } } }").getData();

        System.out.println(queryAnswer);
    }
}
