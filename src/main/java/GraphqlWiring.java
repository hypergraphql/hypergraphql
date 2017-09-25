import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;
import graphql.language.*;
import graphql.schema.*;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static graphql.Scalars.*;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * Created by szymon on 24/08/2017.
 *
 * This class defines the GraphQL wiring (data fetchers and type resolvers)
 *
 */

public class GraphqlWiring {

    //public static Map<String, String> PREFIX_MAP = new HashMap<>(); //from namespaces to prefixes
    //public static Map<String, String> NAME_TO_URI_MAP = new HashMap<>();
    //public static Set<String> GRAPHQL_NAMES = new HashSet<>();

    public Config config;
    public GraphQLSchema schema;
    public Converter converter;


    public GraphqlWiring(Config config) {

        this.config = config;
        this.converter = new Converter(config);

        Set<GraphQLType> schemaTypes = new HashSet<>();
        GraphQLObjectType schemaQuery = null;

        Map<String, TypeDefinition> typeMap = config.schema.types();

        Set<String> typeDefs = typeMap.keySet();

        for (String name : typeDefs) {

            TypeDefinition type = typeMap.get(name);

            if (type.getClass()==ObjectTypeDefinition.class)
                if (name.equals("Query")) schemaQuery = registerGraphQLType(type);
                else schemaTypes.add(registerGraphQLType(type));
        }

        this.schema = GraphQLSchema.newSchema()
                .query(schemaQuery)
                .build(schemaTypes);
    }

    class FetchParams {
        String nodeUri;
        String predicateURI;
        SparqlClient client;

        public FetchParams(DataFetchingEnvironment environment) {
            RDFNode parent = environment.getSource();
            if (parent.isAnon()) {
                nodeUri = "_:" + ((RDFNode) environment.getSource()).asResource().getId();
            } else nodeUri = ((RDFNode) environment.getSource()).asResource().getURI();

            String predicate = ((Field) environment.getFields().toArray()[0]).getName();
            predicateURI = config.context.get(predicate);
            client = environment.getContext();
        }
    }

    private DataFetcher<String> idFetcher = environment -> {
        RDFNode thisNode = environment.getSource();
        if (thisNode.asResource().isURIResource())
        return thisNode.asResource().getURI();
        else return "blank node";
    };

    private DataFetcher<List<RDFNode>> subjectsOfObjectPropertyFetcher = environment -> {
        String object = environment.getArgument("uri");
        String objectUri = config.context.get(object);
        String predicate = ((Field) environment.getFields().toArray()[0]).getName();
        String predicateURI = config.context.get(predicate);
        SparqlClient client = environment.getContext();
        List<RDFNode> subjects = client.getSubjectsOfObjectPropertyFilter(predicateURI, objectUri);
        return subjects;
    };

    private DataFetcher<RDFNode> subjectOfObjectPropertyFetcher = environment -> {
        String object = environment.getArgument("uri");
        String objectUri = config.context.get(object);
        String predicate = ((Field) environment.getFields().toArray()[0]).getName();
        String predicateURI = config.context.get(predicate);
        SparqlClient client = environment.getContext();
        RDFNode subject = client.getSubjectOfObjectPropertyFilter(predicateURI, objectUri);
        return subject;
    };

    private DataFetcher<List<RDFNode>> subjectsOfDataPropertyFetcher = environment -> {
        String value = environment.getArgument("value");
        String predicate = ((Field) environment.getFields().toArray()[0]).getName();
        String predicateURI = config.context.get(predicate);
        SparqlClient client = environment.getContext();
        List<RDFNode> subjects = client.getSubjectsOfDataPropertyFilter(predicateURI, value);
        return subjects;
    };

    private DataFetcher<RDFNode> subjectOfDataPropertyFetcher = environment -> {
        String value = environment.getArgument("value");
        String predicate = ((Field) environment.getFields().toArray()[0]).getName();
        String predicateURI = config.context.get(predicate);
        SparqlClient client = environment.getContext();
        RDFNode subject = client.getSubjectOfDataPropertyFilter(predicateURI, value);
        return subject;
    };

    private DataFetcher<List<RDFNode>> objectsFetcher = environment -> {

        FetchParams params = new FetchParams(environment);
        List<RDFNode> outgoing = params.client.getValuesOfObjectProperty(params.nodeUri, params.predicateURI);
        return outgoing;

    };

    private DataFetcher<RDFNode> objectFetcher = environment -> {

        FetchParams params = new FetchParams(environment);
        RDFNode outgoing = params.client.getValueOfObjectProperty(params.nodeUri, params.predicateURI);

        return outgoing;
    };

    private DataFetcher<List<Object>> literalValuesFetcher = environment -> {

        FetchParams params = new FetchParams(environment);
        List<Object> outgoing = params.client.getValuesOfDataProperty(params.nodeUri, params.predicateURI);

        return outgoing;
    };

    private DataFetcher<Object> literalValueFetcher = environment -> {

        FetchParams params = new FetchParams(environment);
        Object outgoing = params.client.getValueOfDataProperty(params.nodeUri, params.predicateURI);

        return outgoing;
    };

    private GraphQLFieldDefinition _idField = newFieldDefinition()
            .type(GraphQLID)
            .name("_id")
            .dataFetcher(idFetcher).build();


        public GraphQLObjectType registerGraphQLType(TypeDefinition type) {

            JsonNode typeJson = converter.definitionToJson(type);

            JsonNode fieldDefs = typeJson.get("fieldDefinitions");

            List<GraphQLFieldDefinition> builtFields = new ArrayList<>();

            Boolean isQueryType = type.getName().equals("Query");

          for (JsonNode fieldDef : fieldDefs) {
              builtFields.add(registerGraphQLField(isQueryType, fieldDef));
          }

            GraphQLObjectType newObjectType = newObject()
                    .name(type.getName())
                    .fields(builtFields)
                    .field(_idField)
                    .build();

            return newObjectType;
        }


    public GraphQLFieldDefinition registerGraphQLField(Boolean isQueryType, JsonNode fieldDef) {
        

       Boolean isList = fieldDef.get("type").get("_type").asText().equals("ListType");

       GraphQLOutputType refType = getOutputType(fieldDef.get("type"));

       if (isQueryType) {

           if (fieldDef.get("inputValueDefinitions").get(0).get("name").asText().equals("uri")) {
               if (isList) return getBuiltField(fieldDef, refType, subjectsOfObjectPropertyFetcher);
               else return getBuiltField(fieldDef, refType, subjectOfObjectPropertyFetcher);
           }
           if (fieldDef.get("inputValueDefinitions").get(0).get("name").asText().equals("value")) {
               if (isList) return getBuiltField(fieldDef, refType, subjectsOfDataPropertyFetcher);
               else return getBuiltField(fieldDef, refType, subjectOfDataPropertyFetcher);
           }
       } else {

           String fieldName = (refType.getName() != null) ? refType.getName() : fieldDef.get("type").get("type").get("name").asText();

           if (fieldName.equals("String")||fieldName.equals("Int")||fieldName.equals("ID")||fieldName.equals("Boolean"))
               {
                   if (isList) return getBuiltField(fieldDef, refType, literalValuesFetcher);
                   else return getBuiltField(fieldDef, refType, literalValueFetcher);
               } else {
                   if (isList) return getBuiltField(fieldDef, refType, objectsFetcher);
                   else return getBuiltField(fieldDef, refType, objectFetcher);
               }
           }

       return null;
    }

    private GraphQLFieldDefinition getBuiltField(JsonNode fieldDef, GraphQLOutputType refType, DataFetcher fetcher) {

        if (fieldDef.get("inputValueDefinitions").get(0)!=null) {
            GraphQLFieldDefinition field = newFieldDefinition()
                    .name(fieldDef.get("name").asText())
                    .argument(registerGraphQLArgument(fieldDef.get("inputValueDefinitions").get(0)))
                    .type(refType)
                    .dataFetcher(fetcher).build();
            return field;
        } else {
            GraphQLFieldDefinition field = newFieldDefinition()
                    .name(fieldDef.get("name").asText())
                    .type(refType)
                    .dataFetcher(fetcher).build();
            return field;
        }
    }

    private GraphQLOutputType getOutputType(JsonNode outputTypeDef) {

        String outputType = outputTypeDef.get("_type").asText();

        if (outputType.equals("ListType")) {
            return new GraphQLList ( getOutputType(outputTypeDef.get("type")) );
        }

        if (outputType.equals("NonNullType")) {
            return new GraphQLNonNull ( getOutputType(outputTypeDef.get("type")) );
        }

        if (outputType.equals("TypeName")) {
            String typeName = outputTypeDef.get("name").asText();

            switch (typeName) {
                case "String":
                    return GraphQLString;
                case "ID":
                    return GraphQLID;
                case "Int":
                    return GraphQLInt;
                case "Boolean":
                    return GraphQLBoolean;
                default:
                    return new GraphQLTypeReference(typeName);
            }
        }
        return null;
    }

    public GraphQLArgument registerGraphQLArgument(JsonNode argDef) {


        GraphQLArgument arg = new GraphQLArgument(argDef.get("name").asText(), GraphQLString);

        return arg;
    }

/*
    public void oldGraphqlWiring(Config config) {

      //  registerPredicates(config);

        SparqlClient client = new SparqlClient(config);


        DataFetcher<String> idFetch = environment -> {
            RDFuri thisNode = environment.getSource();
            return thisNode._id;
        };

        DataFetcher<String> valueFetch = environment -> {
            RDFliteral thisNode = environment.getSource();
            return thisNode._value;
        };

        DataFetcher<String> langFetch = environment -> {
            RDFliteral thisNode = environment.getSource();
            return thisNode._language;
        };

        DataFetcher<String> typeFetch = environment -> {
            RDFliteral thisNode = environment.getSource();
            return thisNode._type;
        };

        DataFetcher<List<RDFuri>> instanceFetcher = environment -> {

            if (environment.getArgument("type")!=null) {
                int limit = (environment.getArgument("limit")!=null)? environment.getArgument("limit") : 0;
                List<String> instances = client.getInstances(environment.getArgument("type"), limit);
                List<RDFuri> result = new ArrayList<>();
                instances.forEach(instance -> {
                    RDFuri RDFuriNode = new RDFuri();
                    RDFuriNode._id = instance;
                    result.add(RDFuriNode);
                });
                return result;
            }

            if (environment.getArgument("uri")!=null) {
                List<RDFuri> instances = new ArrayList<>();
                RDFuri RDFuriNode = new RDFuri();
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
                        .type(new GraphQLList(new GraphQLTypeReference("RDFuri")))
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
                    .type(new GraphQLList (new GraphQLTypeReference("RDFuri")))
                    .argument(limitArgument)
                    .dataFetcher(outgoingFetcher).build();

            registeredFields.add(field);
        }

        GraphQLObjectType node = newObject()
                .name("RDFuri")
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
*/

}
