import com.fasterxml.jackson.databind.JsonNode;
import graphql.language.*;
import graphql.schema.*;
import org.apache.jena.rdf.model.RDFNode;

import java.util.*;

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

    public Config config;
    public GraphQLSchema schema;
    public Converter converter;

    Map<String, GraphQLArgument> defaultArguments = new HashMap<String, GraphQLArgument>() {{
        put("limit", new GraphQLArgument("limit", GraphQLInt));
        put("offset", new GraphQLArgument("offset", GraphQLInt));
       // put("objectPropertyURI", new GraphQLArgument("objectPropertyURI", GraphQLString));
       // put("dataPropertyURI", new GraphQLArgument("dataPropertyURI", GraphQLString));
       // put("objectURI", new GraphQLArgument("objectURI", GraphQLString));
      //  put("literalValue", new GraphQLArgument("literalValue", GraphQLString));
        put("graph", new GraphQLArgument("graph", GraphQLString));
        put("endpoint", new GraphQLArgument("endpoint", GraphQLString));
        put("lang", new GraphQLArgument("lang", GraphQLString));
    }};

    List<GraphQLArgument> queryArgs = new ArrayList() {{
        add(defaultArguments.get("limit"));
        add(defaultArguments.get("offset"));
       // add(defaultArguments.get("objectPropertyURI"));
       // add(defaultArguments.get("dataPropertyURI"));
       // add(defaultArguments.get("objectURI"));
       // add(defaultArguments.get("literalValue"));
        add(defaultArguments.get("graph"));
        add(defaultArguments.get("endpoint"));
    }};


    List<GraphQLArgument> nonQueryArgs = new ArrayList() {{
        add(defaultArguments.get("graph"));
        add(defaultArguments.get("endpoint"));
    }};

    class OutputTypeSpecification {
        GraphQLOutputType graphQLType;
        String dataType = null;
        Boolean isList = false;
    }

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

    class fetchParams {
        String nodeUri;
        String predicateURI;
        SparqlClient client;

        public fetchParams(DataFetchingEnvironment environment) {
            RDFNode parent = environment.getSource();
            if (parent.isAnon()) {
                nodeUri = "_:" + ((RDFNode) environment.getSource()).asResource().getId();
            } else nodeUri = ((RDFNode) environment.getSource()).asResource().getURI();

            String predicate = ((Field) environment.getFields().toArray()[0]).getName();
            predicateURI = config.context.get("@predicates").get(predicate).get("@id").asText();
            client = environment.getContext();
        }
    }

    private DataFetcher<String> idFetcher = environment -> {
        RDFNode thisNode = environment.getSource();
        if (thisNode.asResource().isURIResource())
        return thisNode.asResource().getURI();
        else return "blank node";
    };

    private DataFetcher<List<RDFNode>> instancesOfTypeFetcher = environment -> {
        String type = ((Field) environment.getFields().toArray()[0]).getName();
        String typeURI = config.context.get("@predicates").get(type).get("@id").asText();
        SparqlClient client = environment.getContext();
        List<RDFNode> subjects = client.getSubjectsOfObjectPropertyFilter("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", typeURI, environment.getArguments());
        return subjects;
    };

   /* private DataFetcher<List<RDFNode>> subjectsOfObjectPropertyFetcher = environment -> {
        String object = environment.getArgument("uri");
        String objectUri = config.context.get(object);
        String predicate = ((Field) environment.getFields().toArray()[0]).getName();
        String predicateURI = config.context.get("@predicates").get(predicate).get("@id").asText();
        SparqlClient client = environment.getContext();
        List<RDFNode> subjects = client.getSubjectsOfObjectPropertyFilter(predicateURI, objectUri);
        return subjects;
    };

    private DataFetcher<RDFNode> subjectOfObjectPropertyFetcher = environment -> {
        String object = environment.getArgument("uri");
        String objectUri = config.context.get(object);
        String predicate = ((Field) environment.getFields().toArray()[0]).getName();
        String predicateURI = config.context.get("@predicates").get(predicate).get("@id").asText();
        SparqlClient client = environment.getContext();
        RDFNode subject = client.getSubjectOfObjectPropertyFilter(predicateURI, objectUri);
        return subject;
    };

    private DataFetcher<List<RDFNode>> subjectsOfDataPropertyFetcher = environment -> {
        String value = environment.getArgument("value");
        String predicate = ((Field) environment.getFields().toArray()[0]).getName();
        String predicateURI = config.context.get("@predicates").get(predicate).get("@id").asText();
        SparqlClient client = environment.getContext();
        List<RDFNode> subjects = client.getSubjectsOfDataPropertyFilter(predicateURI, value);
        return subjects;
    };

    private DataFetcher<RDFNode> subjectOfDataPropertyFetcher = environment -> {
        String value = environment.getArgument("value");
        String predicate = ((Field) environment.getFields().toArray()[0]).getName();
        String predicateURI = config.context.get("@predicates").get(predicate).get("@id").asText();
        SparqlClient client = environment.getContext();
        RDFNode subject = client.getSubjectOfDataPropertyFilter(predicateURI, value);
        return subject;
    };
    */

    private DataFetcher<List<RDFNode>> objectsFetcher = environment -> {

        fetchParams params = new fetchParams(environment);
        List<RDFNode> outgoing = params.client.getValuesOfObjectProperty(params.nodeUri, params.predicateURI, environment.getArguments());
        return outgoing;

    };

    private DataFetcher<RDFNode> objectFetcher = environment -> {

        fetchParams params = new fetchParams(environment);
        RDFNode outgoing = params.client.getValueOfObjectProperty(params.nodeUri, params.predicateURI, environment.getArguments());

        return outgoing;
    };

    private DataFetcher<List<Object>> literalValuesFetcher = environment -> {

        fetchParams params = new fetchParams(environment);
        List<Object> outgoing = params.client.getValuesOfDataProperty(params.nodeUri, params.predicateURI, environment.getArguments());

        return outgoing;
    };

    private DataFetcher<Object> literalValueFetcher = environment -> {

        fetchParams params = new fetchParams(environment);
        Object outgoing = params.client.getValueOfDataProperty(params.nodeUri, params.predicateURI, environment.getArguments());

        return outgoing;
    };

    Map<Boolean, DataFetcher> objectFetchers = new HashMap<Boolean, DataFetcher>() {{
            put(true, objectsFetcher);
            put(false, objectFetcher);
        }};

    Map<Boolean, DataFetcher> literalFetchers = new HashMap<Boolean, DataFetcher>() {{
        put(true, literalValuesFetcher);
        put(false, literalValueFetcher);
    }};

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

          if (!isQueryType) builtFields.add(_idField);

            GraphQLObjectType newObjectType = newObject()
                    .name(type.getName())
                    .fields(builtFields)
                    .build();

            return newObjectType;
        }


    public GraphQLFieldDefinition registerGraphQLField(Boolean isQueryType, JsonNode fieldDef) {
        

       OutputTypeSpecification refType = getOutputType(fieldDef.get("type"));

       if (isQueryType) {
           if (refType.isList) return getBuiltField(isQueryType, fieldDef, refType, instancesOfTypeFetcher);
           else {
               System.out.println("All queries must return array types.");
               System.exit(1);
           }

/*
           if (fieldDef.get("inputValueDefinitions").get(0).get("name").asText().equals("uri")) {
               if (isList) return getBuiltField(fieldDef, refType, subjectsOfObjectPropertyFetcher);
               else return getBuiltField(fieldDef, refType, subjectOfObjectPropertyFetcher);
           }
           if (fieldDef.get("inputValueDefinitions").get(0).get("name").asText().equals("value")) {
               if (isList) return getBuiltField(fieldDef, refType, subjectsOfDataPropertyFetcher);
               else return getBuiltField(fieldDef, refType, subjectOfDataPropertyFetcher);
           } */
       } else {

           String fieldName = refType.dataType;

           if (fieldName.equals("String")||fieldName.equals("Int")||fieldName.equals("ID")||fieldName.equals("Boolean"))
                return getBuiltField(isQueryType, fieldDef, refType, literalFetchers.get(refType.isList));
               else return getBuiltField(isQueryType, fieldDef, refType, objectFetchers.get(refType.isList));
           }

       return null;
    }

    private GraphQLFieldDefinition getBuiltField(Boolean isQueryType, JsonNode fieldDef, OutputTypeSpecification refType, DataFetcher fetcher) {

        List<GraphQLArgument> args = new ArrayList<>();

        if (isQueryType) args.addAll(queryArgs);
        else {
            args.addAll(nonQueryArgs);
            if (refType.dataType.equals("String")) args.add(defaultArguments.get("lang"));
        }

        GraphQLFieldDefinition field = newFieldDefinition()
                .name(fieldDef.get("name").asText())
                .argument(args)
                .description(config.context.get("@predicates").get(fieldDef.get("name").asText()).get("@id").asText())
                .type(refType.graphQLType)
                .dataFetcher(fetcher).build();

        return field;
    }

    private OutputTypeSpecification getOutputType(JsonNode outputTypeDef) {

        String outputType = outputTypeDef.get("_type").asText();

        OutputTypeSpecification outputSpec = new OutputTypeSpecification();

        if (outputType.equals("ListType")) {
            OutputTypeSpecification innerSpec = getOutputType(outputTypeDef.get("type"));
            outputSpec.graphQLType = new GraphQLList ( innerSpec.graphQLType );
            outputSpec.dataType = innerSpec.dataType;
            outputSpec.isList = true;
            return outputSpec ;
        }

        if (outputType.equals("NonNullType")) {
            OutputTypeSpecification innerSpec = getOutputType(outputTypeDef.get("type"));
            outputSpec.graphQLType = new GraphQLNonNull ( innerSpec.graphQLType );
            outputSpec.dataType = innerSpec.dataType;
            return outputSpec ;
        }

        if (outputType.equals("TypeName")) {
            String typeName = outputTypeDef.get("name").asText();

            switch (typeName) {
                case "String": {
                    outputSpec.dataType = "String";
                    outputSpec.graphQLType = GraphQLString;
                }
                case "ID": {
                    outputSpec.dataType = "ID";
                    outputSpec.graphQLType = GraphQLID;
                }
                case "Int": {
                    outputSpec.dataType = "Int";
                    outputSpec.graphQLType = GraphQLInt;
                }
                case "Boolean": {
                    outputSpec.dataType = "Boolean";
                    outputSpec.graphQLType = GraphQLBoolean;
                }
                default: {
                    outputSpec.dataType = typeName;
                    outputSpec.graphQLType = new GraphQLTypeReference(typeName);
                }
            }

            return outputSpec;
        }
        return null;
    }
}
