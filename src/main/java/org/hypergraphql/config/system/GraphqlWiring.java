package org.hypergraphql.config.system;

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

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.log4j.Logger;
import org.hypergraphql.datamodel.ModelContainer;

/**
 * Created by szymon on 24/08/2017.
 * <p>
 * This class defines the GraphQL wiring (data fetchers and type resolvers)
 */

public class GraphqlWiring {

    private HGQLConfig config;
    private GraphQLSchema schema;

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


    public GraphqlWiring() {

        this.config = HGQLConfig.getInstance();

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
        Resource subjectResource;
        Property property;
        ModelContainer client;

        public fetchParams(DataFetchingEnvironment environment) {
            subjectResource = environment.getSource();
            String predicate = ((Field) environment.getFields().toArray()[0]).getName();
            String predicateURI = config.fields().get(predicate).id();
            client = environment.getContext();
            property = client.getPropertyFromUri(predicateURI);
        }
    }

    private DataFetcher<String> idFetcher = environment -> {
        RDFNode thisNode = environment.getSource();
        if (thisNode.asResource().isURIResource()) {
            return thisNode.asResource().getURI();
        } else {

            return "_:"+thisNode.asNode().getBlankNodeLabel();
        }
    };

    private DataFetcher<String> typeFetcher = environment -> {
        String typeName = environment.getParentType().getName();
        String type = (config.types().containsKey(typeName))? config.types().get(typeName).id() : null;
        return type;

    };

    private DataFetcher<List<RDFNode>> instancesOfTypeFetcher = environment -> {
        Field field = (Field) environment.getFields().toArray()[0];
        String predicate = (field.getAlias()!=null) ? field.getAlias() : field.getName();
        ModelContainer client = environment.getContext();
        List<RDFNode> subjects = client.getSubjectsOfObjectPropertyFilter(config.HGQL_TYPE_URI, config.HGQL_QUERY_URI + predicate);
        return subjects;
    };

    private DataFetcher<List<RDFNode>> objectsFetcher = environment -> {

        fetchParams params = new fetchParams(environment);
        return params.client.getValuesOfObjectProperty(params.subjectResource, params.property, environment.getArguments());
    };

    private DataFetcher<RDFNode> objectFetcher = environment -> {

        fetchParams params = new fetchParams(environment);
        return params.client.getValueOfObjectProperty(params.subjectResource, params.property, environment.getArguments());
    };

    private DataFetcher<List<Object>> literalValuesFetcher = environment -> {

        fetchParams params = new fetchParams(environment);
        return params.client.getValuesOfDataProperty(params.subjectResource, params.property, environment.getArguments());
    };

    private DataFetcher<Object> literalValueFetcher = environment -> {

        fetchParams params = new fetchParams(environment);
        return params.client.getValueOfDataProperty(params.subjectResource, params.property, environment.getArguments());
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

        if (config.types().containsKey(typeName)) {
            String uri = config.types().get(typeName).id();
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

        if (config.types().containsKey(typeName)) builtFields.add(_typeField);

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
       // String graphName = (isQueryType) ? config.queryFields().get(fieldName).service().getGraph() : config.fields().get(fieldName).service().getGraph();
        //String endpointURI = (isQueryType) ? config.queryFields().get(fieldName).service().url() : config.fields().get(fieldName).service().url();
        String uri = (isQueryType) ? null : config.fields().get(fieldName).id();
       // String description = uri + " (graph: " + graphName )";


        GraphQLFieldDefinition field = newFieldDefinition()
                .name(fieldName)
                .argument(args)
                //.description(description)
                .type(refType)
                .dataFetcher(fetcher).build();

        return field;
    }

    public GraphQLSchema schema() {
        return schema;
    }
}
