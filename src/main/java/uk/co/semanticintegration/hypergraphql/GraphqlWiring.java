package uk.co.semanticintegration.hypergraphql;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLID;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import com.fasterxml.jackson.databind.JsonNode;
import graphql.language.Field;
import graphql.language.ObjectTypeDefinition;
import graphql.language.TypeDefinition;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;

import java.util.*;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.log4j.Logger;

/**
 * Created by szymon on 24/08/2017.
 * <p>
 * This class defines the GraphQL wiring (data fetchers and type resolvers)
 */

public class GraphqlWiring {

    private Config config;
    private GraphQLSchema schema;
    private Converter converter;

    static Logger logger = Logger.getLogger(GraphqlWiring.class);



    private Map<String, GraphQLArgument> defaultArguments = new HashMap<String, GraphQLArgument>() {{
        put("limit", new GraphQLArgument("limit", GraphQLInt));
        put("offset", new GraphQLArgument("offset", GraphQLInt));
        // put("graph", new GraphQLArgument("graph", GraphQLString));
        // put("endpoint", new GraphQLArgument("endpoint", GraphQLString));
        put("lang", new GraphQLArgument("lang", GraphQLString));
    }};

    private List<GraphQLArgument> queryArgs = new ArrayList<GraphQLArgument>() {{
        add(defaultArguments.get("limit"));
        add(defaultArguments.get("offset"));
        // add(defaultArguments.get("graph"));
        // add(defaultArguments.get("endpoint"));
    }};


    private List<GraphQLArgument> nonQueryArgs = new ArrayList<GraphQLArgument>() {{
//    add(defaultArguments.get("graph"));
//    add(defaultArguments.get("endpoint"));
    }};

    private class OutputTypeSpecification {
        GraphQLOutputType graphQLType;
        String dataType = null;
        Boolean isList = false;
    }

    public GraphqlWiring(Config config) {

        this.config = config;
        this.converter = new Converter(config);

        Set<GraphQLType> schemaTypes = new HashSet<>();
        GraphQLObjectType schemaQuery = null;


        Map<String, TypeDefinition> typeMap = config.schema().types();

        Set<String> typeDefs = typeMap.keySet();

        for (String name : typeDefs) {

            TypeDefinition type = typeMap.get(name);

            if (type.getClass() == ObjectTypeDefinition.class)
                if (name.equals("Query")) {
                    schemaQuery = registerGraphQLType(type);
                } else {
                    schemaTypes.add(registerGraphQLType(type));
                }
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
            } else {
                nodeUri = ((RDFNode) environment.getSource()).asResource().getURI();
            }

            String predicate = ((Field) environment.getFields().toArray()[0]).getName();
            predicateURI = config.predicateURI(predicate);
            client = environment.getContext();
        }
    }

    private DataFetcher<String> idFetcher = environment -> {
        RDFNode thisNode = environment.getSource();
        if (thisNode.asResource().isURIResource()) {
            return thisNode.asResource().getURI();
        } else {
            UUID uuid = UUID.randomUUID();
            return "_:"+ uuid;
        }
    };

    private DataFetcher<String> typeFetcher = environment -> {

        SparqlClient client = environment.getContext();
        String type = client.getRootTypeOfResource(environment.getSource());
        return type;

    };

    private DataFetcher<List<RDFNode>> instancesOfTypeFetcher = environment -> {
        String type = ((Field) environment.getFields().toArray()[0]).getName();
        String typeURI = config.predicateURI(type);
        SparqlClient client = environment.getContext();
        List<RDFNode> subjects = client.getSubjectsOfObjectPropertyFilter("http://hgql/root", typeURI);
        return subjects;
    };

    private DataFetcher<List<RDFNode>> objectsFetcher = environment -> {

        fetchParams params = new fetchParams(environment);
        return params.client.getValuesOfObjectProperty(params.nodeUri, params.predicateURI, environment.getArguments());
    };

    private DataFetcher<RDFNode> objectFetcher = environment -> {

        fetchParams params = new fetchParams(environment);
        return params.client.getValueOfObjectProperty(params.nodeUri, params.predicateURI, environment.getArguments());
    };

    private DataFetcher<List<Object>> literalValuesFetcher = environment -> {

        fetchParams params = new fetchParams(environment);
        return params.client.getValuesOfDataProperty(params.nodeUri, params.predicateURI, environment.getArguments());
    };

    private DataFetcher<Object> literalValueFetcher = environment -> {

        fetchParams params = new fetchParams(environment);
        return params.client.getValueOfDataProperty(params.nodeUri, params.predicateURI, environment.getArguments());
    };

    private Map<Boolean, DataFetcher> objectFetchers = new HashMap<Boolean, DataFetcher>() {{
        put(true, objectsFetcher);
        put(false, objectFetcher);
    }};

    private Map<Boolean, DataFetcher> literalFetchers = new HashMap<Boolean, DataFetcher>() {{
        put(true, literalValuesFetcher);
        put(false, literalValueFetcher);
    }};

    private GraphQLFieldDefinition _idField = newFieldDefinition()
            .type(GraphQLID)
            .name("_id")
            .description("The URI of this resource.")
            .dataFetcher(idFetcher).build();

    private GraphQLFieldDefinition _typeField = newFieldDefinition()
            .type(GraphQLID)
            .name("_type")
            .description("The rdf:type of this resource via which it was retrieved from the SPARQL endpoint. " +
                    "Note that this field is applicable only to root resources. For all others it will be set to null," +
                    "which might result in errors when processing the JSON-LD response.")
            .dataFetcher(typeFetcher).build();


    public GraphQLObjectType registerGraphQLType(TypeDefinition type) {

        JsonNode typeJson = converter.definitionToJson(type);

        JsonNode fieldDefs = typeJson.get("fieldDefinitions");

        List<GraphQLFieldDefinition> builtFields = new ArrayList<>();

        Boolean isQueryType = type.getName().equals("Query");

        for (JsonNode fieldDef : fieldDefs) {
            builtFields.add(registerGraphQLField(isQueryType, fieldDef));
        }

        if (!isQueryType) {
            builtFields.add(_idField);
            builtFields.add(_typeField);
        }


        GraphQLObjectType newObjectType = newObject()
                .name(type.getName())
                .fields(builtFields)
                .build();

        return newObjectType;
    }


    public GraphQLFieldDefinition registerGraphQLField(Boolean isQueryType, JsonNode fieldDef) {

        OutputTypeSpecification refType = outputType(fieldDef.get("type"));

        if (isQueryType) {
            if (refType.isList) {
                return getBuiltField(isQueryType, fieldDef, refType, instancesOfTypeFetcher);
            } else {
                logger.error("Wrong schema: all fields in the Query type must return array types. This is not the case for: " + fieldDef.get("name"));
                System.exit(1);
            }
        } else {

            String targetDataTypeName = refType.dataType;

            final Set<String> allowedTypes = new HashSet<String>() {{
                add("String");
                add("Int");
                add("ID");
                add("Boolean");
            }};

            if (allowedTypes.contains(targetDataTypeName)) {
                return getBuiltField(isQueryType, fieldDef, refType, literalFetchers.get(refType.isList));
            } else {
                return getBuiltField(isQueryType, fieldDef, refType, objectFetchers.get(refType.isList));
            }
        }

        return null;

    }

    private GraphQLFieldDefinition getBuiltField(Boolean isQueryType, JsonNode fieldDef, OutputTypeSpecification refType, DataFetcher fetcher) {

        List<GraphQLArgument> args = new ArrayList<>();

        if (isQueryType) {
            args.addAll(queryArgs);
        } else {
            args.addAll(nonQueryArgs);
            if (refType.dataType.equals("String")) {
                args.add(defaultArguments.get("lang"));
            }
        }

        String description;
        String fieldName = fieldDef.get("name").asText();
        String uri = config.predicateURI(fieldName);
        String graphName = config.predicateGraph(fieldName);
        String endpointURI = config.predicateEndpoint(fieldName);
        String descString = uri + " (graph: " + graphName + "; endpoint: " + endpointURI + ")";

        if (isQueryType) {
            description = "Instances of " + descString;
        } else {
            description = "Values of " + descString;
        }

        GraphQLFieldDefinition field = newFieldDefinition()
                .name(fieldDef.get("name").asText())
                .argument(args)
                .description(description)
                .type(refType.graphQLType)
                .dataFetcher(fetcher).build();

        return field;
    }

    private OutputTypeSpecification outputType(JsonNode outputTypeDef) {

        String outputType = outputTypeDef.get("_type").asText();

        OutputTypeSpecification outputSpec = new OutputTypeSpecification();

        if (outputType.equals("ListType")) {
            OutputTypeSpecification innerSpec = outputType(outputTypeDef.get("type"));
            outputSpec.graphQLType = new GraphQLList(innerSpec.graphQLType);
            outputSpec.dataType = innerSpec.dataType;
            outputSpec.isList = true;
            return outputSpec;
        }

        if (outputType.equals("NonNullType")) {
            OutputTypeSpecification innerSpec = outputType(outputTypeDef.get("type"));
            outputSpec.graphQLType = new GraphQLNonNull(innerSpec.graphQLType);
            outputSpec.dataType = innerSpec.dataType;
            return outputSpec;
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

    public GraphQLSchema schema() {
        return schema;
    }
}
