import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.language.*;
import graphql.schema.*;
import org.apache.jena.rdf.model.RDFNode;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * Created by szymon on 24/08/2017.
 */
public class GraphqlWiring {

    //public static Map<String, String> PREFIX_MAP = new HashMap<>(); //from namespaces to prefixes
    //public static Map<String, String> NAME_TO_URI_MAP = new HashMap<>();
    //public static Set<String> GRAPHQL_NAMES = new HashSet<>();

    public SparqlClient client;
    public Config config;
    public GraphQLSchema schema;

    private DataFetcher<String> idFetcher = environment -> {
        RDFNode thisNode = environment.getSource();
        return thisNode.asResource().getURI();
    };

    private DataFetcher<List<RDFNode>> subjectsOfObjectPropertyFetcher = environment -> {
        String object = environment.getArgument("uri");
        String objectUri = config.context.get(object);
        String predicate = ((Field) environment.getFields().toArray()[0]).getName();
        String predicateURI = config.context.get(predicate);
        System.out.println(predicateURI);
        List<RDFNode> subjects = client.getSubjectsOfObjectPropertyFilter(predicateURI, objectUri);
        return subjects;
    };

    private DataFetcher<RDFNode> subjectOfObjectPropertyFetcher = environment -> {
        String object = environment.getArgument("uri");
        String objectUri = config.context.get(object);
        String predicate = ((Field) environment.getFields().toArray()[0]).getName();
        String predicateURI = config.context.get(predicate);
        RDFNode subject = client.getSubjectOfObjectPropertyFilter(predicateURI, objectUri);
        return subject;
    };

    private DataFetcher<List<RDFNode>> subjectsOfDataPropertyFetcher = environment -> {
        String value = environment.getArgument("value");
        String predicate = ((Field) environment.getFields().toArray()[0]).getName();
        String predicateURI = config.context.get(predicate);
        List<RDFNode> subjects = client.getSubjectsOfDataPropertyFilter(predicateURI, value);
        return subjects;
    };

    private DataFetcher<RDFNode> subjectOfDataPropertyFetcher = environment -> {
        String value = environment.getArgument("value");
        String predicate = ((Field) environment.getFields().toArray()[0]).getName();
        String predicateURI = config.context.get(predicate);
        RDFNode subject = client.getSubjectOfDataPropertyFilter(predicateURI, value);
        return subject;
    };

    private DataFetcher<List<RDFNode>> objectsFetcher = environment -> {

        String nodeUri = ((RDFNode) environment.getSource()).asResource().getURI();
        String predicate = ((Field) environment.getFields().toArray()[0]).getName();
        String predicateURI = config.context.get(predicate);
        List<RDFNode> outgoing = client.getValuesOfObjectProperty(nodeUri, predicateURI);
        return outgoing;
    };

    private DataFetcher<RDFNode> objectFetcher = environment -> {

        String nodeUri = ((RDFNode) environment.getSource()).asResource().getURI();
        String predicate = ((Field) environment.getFields().toArray()[0]).getName();
        String predicateURI = config.context.get(predicate);
        RDFNode outgoing = client.getValueOfObjectProperty(nodeUri, predicateURI);
        return outgoing;
    };

    private DataFetcher<List<String>> stringValuesFetcher = environment -> {
        String nodeUri = ((RDFNode) environment.getSource()).asResource().getURI();
        String predicate = ((Field) environment.getFields().toArray()[0]).getName();
        String predicateURI = config.context.get(predicate);
        List<String> outgoing = client.getValuesOfDataProperty(nodeUri, predicateURI);
        return outgoing;
    };

    private DataFetcher<String> stringValueFetcher = environment -> {
        String nodeUri = ((RDFNode) environment.getSource()).asResource().getURI();
        String predicate = ((Field) environment.getFields().toArray()[0]).getName();
        String predicateURI = config.context.get(predicate);
        String outgoing = client.getValueOfDataProperty(nodeUri, predicateURI);
        return outgoing;
    };

    private DataFetcher<List<Integer>> integerValuesFetcher = environment -> {
        String nodeUri = ((RDFNode) environment.getSource()).asResource().getURI();
        String predicate = ((Field) environment.getFields().toArray()[0]).getName();
        String predicateURI = config.context.get(predicate);
        List<String> outgoing = client.getValuesOfDataProperty(nodeUri, predicateURI);
        List<Integer> result = new ArrayList<>();
        outgoing.forEach(string -> result.add(Integer.valueOf(string)));
        return result;
    };

    private DataFetcher<Integer> integerValueFetcher = environment -> {
        String nodeUri = ((RDFNode) environment.getSource()).asResource().getURI();
        String predicate = ((Field) environment.getFields().toArray()[0]).getName();
        String predicateURI = config.context.get(predicate);
        String outgoing = client.getValueOfDataProperty(nodeUri, predicateURI);
        Integer result = Integer.valueOf(outgoing);
        return result;
    };

    private DataFetcher<List<Boolean>> booleanValuesFetcher = environment -> {
        String nodeUri = ((RDFNode) environment.getSource()).asResource().getURI();
        String predicate = ((Field) environment.getFields().toArray()[0]).getName();
        String predicateURI = config.context.get(predicate);
        List<String> outgoing = client.getValuesOfDataProperty(nodeUri, predicateURI);
        List<Boolean> result = new ArrayList<>();
        outgoing.forEach(string -> result.add(Boolean.valueOf(string)));
        return result;
    };

    private DataFetcher<Boolean> booleanValueFetcher = environment -> {
        String nodeUri = ((RDFNode) environment.getSource()).asResource().getURI();
        String predicate = ((Field) environment.getFields().toArray()[0]).getName();
        String predicateURI = config.context.get(predicate);
        String outgoing = client.getValueOfDataProperty(nodeUri, predicateURI);
        Boolean result = Boolean.valueOf(outgoing);
        return result;
    };

    private GraphQLFieldDefinition _idField = newFieldDefinition()
            .type(GraphQLString)
            .name("_id")
            .dataFetcher(idFetcher).build();

    public GraphqlWiring(Config config) {

        this.config = config;
        this.client = new SparqlClient(config);



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


        public GraphQLObjectType registerGraphQLType(TypeDefinition type) {

           // //System.out.println(type);
            JsonNode typeJson = convertToJson(type);

            //System.out.println("Registering type: " + typeJson.toString());

            JsonNode fieldDefs = typeJson.get("fieldDefinitions");

            List<GraphQLFieldDefinition> builtFields = new ArrayList<>();

            Boolean isQueryType = type.getName().equals("Query");

          for (JsonNode fieldDef : fieldDefs) {
              builtFields.add(registerGraphQLField(isQueryType, fieldDef));
          }

          //System.out.println("\nNow registering type:");
          //System.out.println("\t"+type.getName());
            //System.out.println("\t"+builtFields.toString());
            //System.out.println("\t"+_idField.toString());

            GraphQLObjectType newObjectType = newObject()
                    .name(type.getName())
                    .fields(builtFields)
                    .field(_idField)
                    .build();


            return newObjectType;
        }

    private JsonNode convertToJson(TypeDefinition type) {

        String typeData = type.toString();
        Pattern namePtrn = Pattern.compile("(\\w+)\\{");
        Matcher nameMtchr = namePtrn.matcher(typeData);

        while (nameMtchr.find()) {
            String find = nameMtchr.group(1);
            typeData = typeData.replace(find + "{", "{\'_type\':\'" + find + "\', ");
        }

        namePtrn = Pattern.compile("(\\w+)=");
        nameMtchr = namePtrn.matcher(typeData);

        while (nameMtchr.find()) {
            String find = nameMtchr.group(1);
            typeData = typeData.replace(" " + find + "=", "\'"+find+"\':");
        }

        typeData = typeData.replace("'", "\"");

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode object = mapper.readTree(typeData);

            return object;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }


    public GraphQLFieldDefinition registerGraphQLField(Boolean isQueryType, JsonNode fieldDef) {
        
       //System.out.println("\nRegistering field: "+ fieldDef.toString());

       Boolean isList = fieldDef.get("type").get("_type").asText().equals("ListType");

       GraphQLOutputType refType = getOutputType(fieldDef.get("type"));

        //System.out.println(isList);
        //System.out.println(isQueryType);

       if (isQueryType) {
           //System.out.println(fieldDef.get("inputValueDefinitions").toString());
           //System.out.println(fieldDef.get("inputValueDefinitions").get(0).toString());

           if (fieldDef.get("inputValueDefinitions").get(0).get("name").asText().equals("uri")) {
               if (isList) return getBuiltField(fieldDef, refType, subjectsOfObjectPropertyFetcher);
               else return getBuiltField(fieldDef, refType, subjectOfObjectPropertyFetcher);
           }
           if (fieldDef.get("inputValueDefinitions").get(0).get("name").asText().equals("value")) {
               if (isList) return getBuiltField(fieldDef, refType, subjectsOfDataPropertyFetcher);
               else return getBuiltField(fieldDef, refType, subjectOfDataPropertyFetcher);
           }
       } else {
           String fieldName = (refType.getName()!=null) ? refType.getName() : fieldDef.get("type").get("type").get("name").asText();
            if (fieldName.equals("String")) {
                if (isList) return getBuiltField(fieldDef, refType, stringValuesFetcher);
                else return getBuiltField(fieldDef, refType, stringValueFetcher);
            }
           if (fieldName.equals("ID")) {
               if (isList) return getBuiltField(fieldDef, refType, stringValuesFetcher);
               else return getBuiltField(fieldDef, refType, stringValueFetcher);
           }
           if (fieldName.equals("Int")) {
               if (isList) return getBuiltField(fieldDef, refType, integerValuesFetcher);
               else return getBuiltField(fieldDef, refType, integerValueFetcher);
           }
           if (fieldName.equals("Boolean")) {
               if (isList) return getBuiltField(fieldDef, refType, booleanValuesFetcher);
               else return getBuiltField(fieldDef, refType, booleanValueFetcher);
           }
           else {
               if (isList) return getBuiltField(fieldDef, refType, objectsFetcher);
               else return getBuiltField(fieldDef, refType, objectFetcher);
           }

       }

       return null;
    }

    private GraphQLFieldDefinition getBuiltField(JsonNode fieldDef, GraphQLOutputType refType, DataFetcher fetcher) {

        //System.out.println("\nNow building field:");
        //System.out.println("\t"+fieldDef.get("name").asText());
//        //System.out.println("\t"+registerGraphQLArgument(fieldDef.get("inputValueDefinitions").get(0)).toString());
        //System.out.println("\t"+refType.toString());
        //System.out.println("\t"+fetcher.toString());

        if (fieldDef.get("inputValueDefinitions").get(0)!=null) {
            GraphQLFieldDefinition field = newFieldDefinition()
                    .name(fieldDef.get("name").asText())
                    .argument(registerGraphQLArgument(fieldDef.get("inputValueDefinitions").get(0)))
                    .type(refType)
                    .dataFetcher(fetcher).build();

            //System.out.println("Registered field: " + field.toString());
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

        //System.out.println("Getting output type: " + outputTypeDef.toString());

        if (outputTypeDef.get("_type").asText().equals("ListType"))
        return new GraphQLList (new GraphQLTypeReference(outputTypeDef.get("type").get("name").asText()));
        else if (outputTypeDef.get("_type").asText().equals("NonNullType"))
            return new GraphQLTypeReference(outputTypeDef.get("type").get("name").asText());
        else return new GraphQLTypeReference(outputTypeDef.get("name").asText());

    }

    public GraphQLArgument registerGraphQLArgument(JsonNode argDef) {

        //System.out.println("Trying to register argument: " + argDef);

        GraphQLArgument arg = new GraphQLArgument(argDef.get("name").asText(), GraphQLString);

        //System.out.println("Registered arg: " + arg.toString());

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
