package org.hypergraphql;

import static graphql.Scalars.GraphQLID;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import com.fasterxml.jackson.databind.JsonNode;
import graphql.language.Field;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;

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


    public GraphqlWiring(Config config) {

        this.config = config;
        this.converter = new Converter(config);

        Set<GraphQLType> types = new HashSet<>();
        GraphQLObjectType queryType = null;

        JsonNode mapping = config.mapping();

        Iterator<String> typeNames = mapping.fieldNames();

        while(typeNames.hasNext()) {
            String typeName = typeNames.next();
            GraphQLObjectType typeDef = registerGraphQLType(mapping.get(typeName));

            if (typeName.equals("Query")) {
                queryType = typeDef;
            } else {
                types.add(typeDef);
            }
        }

        this.schema = GraphQLSchema.newSchema()
                .query(queryType)
                .build(types);

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
        String typeName = environment.getParentType().getName();
        String type = (config.containsPredicate(typeName))? config.predicateURI(typeName) : null;
        return type;

    };

    private DataFetcher<List<RDFNode>> instancesOfTypeFetcher = environment -> {
        String type = ((Field) environment.getFields().toArray()[0]).getName();
        SparqlClient client = environment.getContext();
        List<RDFNode> subjects = client.getSubjectsOfObjectPropertyFilter("http://hypergraphql/type", "http://hypergraphql/query/" + type);
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
            .description("The rdf:type of this resource that was used as a filter when querying SPARQL endpoint. ")
            .dataFetcher(typeFetcher).build();


    public GraphQLObjectType registerGraphQLType(JsonNode type) {

        JsonNode fields = type.get("fields");

        String typeName = type.get("name").asText();

        Boolean isQueryType = typeName.equals("Query");

        String description;

        if (config.containsPredicate(typeName)) {
            String uri = config.predicateURI(typeName);
            description = uri;
        } else {
            description = "An auxiliary type, not mapped to RDF.";
        }

        List<GraphQLFieldDefinition> builtFields = new ArrayList<>();

        for (JsonNode field : fields) {
            builtFields.add(registerGraphQLField(isQueryType, field));
        }

        if (!isQueryType) {
            builtFields.add(_idField);
        }

        if (config.containsPredicate(typeName)) builtFields.add(_typeField);

        GraphQLObjectType newObjectType = newObject()
                .name(typeName)
                .description(description)
                .fields(builtFields)
                .build();

        return newObjectType;
    }

    public GraphQLFieldDefinition registerGraphQLField(Boolean isQueryType, JsonNode fieldDef) {

        Boolean isList = fieldDef.get("targetInList").asBoolean();
        if (isQueryType) {
            if (isList) {
                return getBuiltField(isQueryType, fieldDef, instancesOfTypeFetcher);
            } else {
                logger.error("Wrong schema: all fields in the Query type must return array types. This is not the case for: " + fieldDef.get("name"));
                System.exit(1);
            }
        } else {

            if (fieldDef.get("targetKind").asText().equals("SCALAR")) {
                return getBuiltField(isQueryType, fieldDef, literalFetchers.get(isList));
            } else {
                return getBuiltField(isQueryType, fieldDef, objectFetchers.get(isList));
            }
        }

        return null;

    }

    private GraphQLFieldDefinition getBuiltField(Boolean isQueryType, JsonNode fieldDef, DataFetcher fetcher) {

        GraphQLOutputType refType = config.outputType(fieldDef.get("targetTypeId").asText());

        List<GraphQLArgument> args = new ArrayList<>();

        if (isQueryType) {
            args.addAll(queryArgs);
        } else {
            args.addAll(nonQueryArgs);
            if (fieldDef.get("targetName").asText().equals("String")) {
                args.add(defaultArguments.get("lang"));
            }
        }

        String fieldName = fieldDef.get("name").asText();
        String graphName = config.predicateGraph(fieldName);
        String endpointURI = config.predicateEndpoint(fieldName);
        String uri = (config.containsPredicate(fieldName)) ? config.predicateURI(fieldName) : "";
        String description = uri + " (graph: " + graphName + "; endpoint: " + endpointURI + ")";


        GraphQLFieldDefinition field = newFieldDefinition()
                .name(fieldName)
                .argument(args)
                .description(description)
                .type(refType)
                .dataFetcher(fetcher).build();

        return field;
    }

    public GraphQLSchema schema() {
        return schema;
    }
}
