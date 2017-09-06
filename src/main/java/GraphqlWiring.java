import graphql.language.Field;
import graphql.schema.*;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import java.util.*;

import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * Created by szymon on 24/08/2017.
 */
public class GraphqlWiring {

    GraphQLSchema schema;
    public static Map<String, String> PREFIX_MAP = new HashMap<>(); //from namespaces to prefixes
    public static Map<String, String> NAME_TO_URI_MAP = new HashMap<>();
    public static Set<String> GRAPHQL_NAMES = new HashSet<>();


    public GraphqlWiring(Config config) {

        registerPredicates(config);

        SparqlClient client = new SparqlClient(config);


        DataFetcher<String> idFetch = environment -> {
            RDFuriNode thisNode = environment.getSource();
            return thisNode._id;
        };

        DataFetcher<String> valueFetch = environment -> {
            RDFliteralNode thisNode = environment.getSource();
            return thisNode._value;
        };

        DataFetcher<String> langFetch = environment -> {
            RDFliteralNode thisNode = environment.getSource();
            return thisNode._language;
        };

        DataFetcher<String> typeFetch = environment -> {
            RDFliteralNode thisNode = environment.getSource();
            return thisNode._type;
        };

        DataFetcher<List<Object>> outgoingFetcher = environment -> {

            int limit = (environment.getArgument("limit")!=null)? environment.getArgument("limit") : 0;

            String nodeUri = ((RDFuriNode) environment.getSource())._id;
            String predicate = ((Field) environment.getFields().toArray()[0]).getName();
            String predicateURI = NAME_TO_URI_MAP.get(predicate);
            List<RDFNode> outgoing = client.getOutgoingEdge(nodeUri, predicateURI, limit);
            List<Object> result = new ArrayList<>();
            outgoing.forEach(instance -> {
                if (instance.isURIResource()) {
                    RDFuriNode thisNode = new RDFuriNode();
                    thisNode._id = instance.toString();
                    result.add(thisNode);
                }
                if (instance.isLiteral()) {
                    RDFliteralNode thisNode = new RDFliteralNode();
                    thisNode._value = instance.asLiteral().getValue().toString();
                    thisNode._language = instance.asLiteral().getLanguage();
                    thisNode._type = instance.asLiteral().getDatatypeURI();
                    result.add(thisNode);
                }
            });
            return result;
        };

        DataFetcher<List<RDFuriNode>> instanceFetcher = environment -> {

            if (environment.getArgument("type")!=null) {
                int limit = (environment.getArgument("limit")!=null)? environment.getArgument("limit") : 0;
                List<String> instances = client.getInstances(environment.getArgument("type"), limit);
                List<RDFuriNode> result = new ArrayList<>();
                instances.forEach(instance -> {
                    RDFuriNode RDFuriNode = new RDFuriNode();
                    RDFuriNode._id = instance;
                    result.add(RDFuriNode);
                });
                return result;
            }

            if (environment.getArgument("uri")!=null) {
                List<RDFuriNode> instances = new ArrayList<>();
                RDFuriNode RDFuriNode = new RDFuriNode();
                RDFuriNode._id = environment.getArgument("uri");
                instances.add(RDFuriNode);
                return instances;
            }
            else return null;
        };

        GraphQLArgument typeArgument = new GraphQLArgument("type", GraphQLString);
        GraphQLArgument uriArgument = new GraphQLArgument("uri", GraphQLString);
        GraphQLArgument limitArgument = new GraphQLArgument("limit", GraphQLInt);

        GraphQLObjectType RootQuery = newObject()
                .name("RootQuery")
                .field(newFieldDefinition()
                        .type(new GraphQLList(new GraphQLTypeReference("RDFuriNode")))
                        .name("_graph")
                        .argument(typeArgument)
                        .argument(uriArgument)
                        .argument(limitArgument)
                        .dataFetcher(instanceFetcher))
                .build();


        List<GraphQLFieldDefinition> registeredFields = new ArrayList<>();


        for (String name : GRAPHQL_NAMES) {
            GraphQLFieldDefinition field = newFieldDefinition()
                    .name(name)
                    .type(new GraphQLList (new GraphQLTypeReference("RDFuriNode")))
                    .argument(limitArgument)
                    .dataFetcher(outgoingFetcher).build();

            registeredFields.add(field);
        }

        GraphQLObjectType node = newObject()
                .name("RDFuriNode")
                .fields(registeredFields)
                .field(newFieldDefinition()
                        .name("_id")
                        .type(GraphQLString)
                        .dataFetcher(idFetch))
                .field(newFieldDefinition()
                        .name("_value")
                        .type(GraphQLString)
                        .dataFetcher(valueFetch))
                .field(newFieldDefinition()
                        .name("_language")
                        .type(GraphQLString)
                        .dataFetcher(langFetch))
                .field(newFieldDefinition()
                        .name("_type")
                        .type(GraphQLString)
                        .dataFetcher(typeFetch))
                .build();

        Set<GraphQLType> types = new HashSet<>();
        types.add(node);

        this.schema = GraphQLSchema.newSchema()
                .query(RootQuery)
                .build(types);
    }



    //this approach of dynamically generating schema directly from the endpoint
    //will be abondened
    // PJC - probably worth keeping it in place as a default / quickstart option
    public static void registerPredicates(Config config) {
        int count = 0;

        SparqlClient client = new SparqlClient(config);
        String queryString = "select distinct ?predicate where {[] ?predicate []}";
        ResultSet results = client.sparqlSelect(queryString);

        if (results == null) return;
        else
            while (results.hasNext()) {
                QuerySolution nextSol = results.nextSolution();
                Resource predicate = nextSol.get("?predicate").asResource();
                String namespace = predicate.getNameSpace().toString();
                if (!PREFIX_MAP.containsKey(namespace)) {
                    String newPrefix = "ns" + count;
                    count++;
                    PREFIX_MAP.put(namespace, newPrefix);
                }

                String prefix = PREFIX_MAP.get(namespace);
                String localName = predicate.getLocalName().toString();
                String graphqlName = prefix + "_" + localName;

                NAME_TO_URI_MAP.put(graphqlName, predicate.toString());
                GRAPHQL_NAMES.add(graphqlName);

             //   String jsonldName = prefix + ":" + localName;

           //     NAME_MAP.put(graphqlName, jsonldName);
            }
    }

}
