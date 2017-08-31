import com.fasterxml.jackson.databind.JsonNode;
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
public class RDFSchema {

    GraphQLSchema schema;
    public static Map<String, String> PREFIX_MAP = new HashMap<>(); //from namespaces to prefixes
    public static Map<String, String> NAME_MAP = new HashMap<String, String>() {{
        put("_context", "@context");
        put("_id", "@id");
        put("_value", "@value");
        put("_type", "@type");
        put("_language", "@language");
        put("_graph", "@graph");
    }}; //from graphql names to jsonld keys
    public static Map<String, String> NAME_TO_URI_MAP = new HashMap<>();
    public static Set<String> GRAPHQL_NAMES = new HashSet<>();


    public RDFSchema() {

        registerPredicates();

        DataFetcher<String> idFetch = environment -> {
            Node thisNode = environment.getSource();
            return thisNode._id;
        };

        DataFetcher<String> valueFetch = environment -> {
            Node thisNode = environment.getSource();
            return thisNode._value;
        };

        DataFetcher<String> langFetch = environment -> {
            Node thisNode = environment.getSource();
            return thisNode._language;
        };

        DataFetcher<String> typeFetch = environment -> {
            Node thisNode = environment.getSource();
            return thisNode._type;
        };

        DataFetcher<List<Node>> outgoingFetcher = environment -> {

            int limit = (environment.getArgument("limit")!=null)? environment.getArgument("limit") : 0;

            String nodeUri = ((Node) environment.getSource())._id;
            String predicate = ((Field) environment.getFields().toArray()[0]).getName();
            String predicateURI = NAME_TO_URI_MAP.get(predicate);
            List<RDFNode> outgoing = SparqlClient.getOutgoingEdge(nodeUri, predicateURI, limit);
            List<Node> result = new ArrayList<>();
            outgoing.forEach(instance -> {
                Node node = new Node();
                if (instance.isURIResource()) node._id = instance.toString();
                if (instance.isLiteral()) {
                    node._value = instance.asLiteral().getValue().toString();
                    node._language = instance.asLiteral().getLanguage();
                    node._type = instance.asLiteral().getDatatypeURI();
                }
                result.add(node);
            });
            return result;
        };

        DataFetcher<List<Node>> instanceFetcher = environment -> {

            if (environment.getArgument("type")!=null) {
                int limit = (environment.getArgument("limit")!=null)? environment.getArgument("limit") : 0;
                List<String> instances = SparqlClient.getInstances(environment.getArgument("type"), limit);
                List<Node> result = new ArrayList<>();
                instances.forEach(instance -> {
                    Node node = new Node();
                    node._id = instance;
                    result.add(node);
                });
                return result;
            }

            if (environment.getArgument("uri")!=null) {
                List<Node> instances = new ArrayList<>();
                Node node = new Node();
                node._id = environment.getArgument("uri");
                instances.add(node);
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
                        .type(new GraphQLList(new GraphQLTypeReference("Node")))
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
                    .type(new GraphQLList (new GraphQLTypeReference("Node")))
                    .argument(limitArgument)
                    .dataFetcher(outgoingFetcher).build();

            registeredFields.add(field);
        }

        GraphQLObjectType node = newObject()
                .name("Node")
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

    public static void registerPredicates() {
        int count = 0;
        String queryString = "select distinct ?predicate where {[] ?predicate []}";
        ResultSet results = SparqlClient.sparqlSelect(queryString);

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


    public static Traversal restructure (Object dataObject) {

       // System.out.println("Transforming this: " + dataObject.toString());
       // System.out.println("And I know it's a... : " + dataObject.getClass().toString());

        if (dataObject.getClass()==ArrayList.class) {
         //   System.out.println("It's an array");
            ArrayList<Object> newList = new ArrayList<>();
            Map<String, String> context = new HashMap<>();
            for (Object item : (ArrayList<Object>) dataObject) {
                Traversal itemRes = restructure(item);
                newList.add(itemRes.data);
                context.putAll(itemRes.context);
            }
            Traversal restructured = new Traversal();
            restructured.data = newList;
            restructured.context = context;

            return restructured;
        }

        if (dataObject.getClass()!=LinkedHashMap.class) {
           // System.out.println("It's not an array nor a map.");
            Traversal restructured = new Traversal();
            restructured.data = dataObject;
            restructured.context = new HashMap<>();

            return restructured;
        } else {

           // System.out.println("It's a map.");

            Map<String, Object> mapObject = (Map<String, Object>) dataObject;
            Map<String, Object> targetObject = new HashMap<>();

            Set<String> keys = mapObject.keySet();

            Map<String, String> context = new HashMap<>();

            for (String key: keys) {
                Object child = mapObject.get(key);
                Traversal childRes = restructure(child);
                if (NAME_MAP.containsKey(key)) targetObject.put(NAME_MAP.get(key), childRes.data);
                else {
                    targetObject.put(key, childRes.data);
                    context.put(key, NAME_TO_URI_MAP.get(key));
                }
                context.putAll(childRes.context);
            }

            Traversal restructured = new Traversal();
            restructured.data = targetObject;
            restructured.context = context;

            return restructured;
        }

    }

 public static Object graphql2jsonld (Map<String, Object> dataObject) {

        if (dataObject.containsKey("_graph")) {
      //      System.out.println("Original data object:");
        //    System.out.println(dataObject.toString());
            Traversal restructured = restructure(dataObject.get("_graph"));
       //     System.out.println("Transformed object:");
            Map<String, Object> output = new HashMap<>();

            output.put("@graph", restructured.data);
            output.put("@context", restructured.context);

            System.out.println(output.toString());

            return output;

        }

        return null;
    }


}
