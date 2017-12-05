package org.hypergraphql.config.schema;

import static graphql.Scalars.GraphQLID;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;
import graphql.language.Field;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.log4j.Logger;
import org.hypergraphql.datafetching.services.Service;
import org.hypergraphql.datamodel.ModelContainer;

/**
 * Created by szymon on 24/08/2017.
 * <p>
 * This class defines the GraphQL wiring (data fetchers and type resolvers)
 */

public class HGQLSchemaConfig {

    static Logger logger = Logger.getLogger(HGQLSchemaConfig.class);


    private static HGQLSchemaConfig instance = null;
    private GraphQLSchema schema;
    private GraphQL graphql;
    private JsonNode schemaJson;

    private Map<String, GraphQLArgument> defaultArguments = new HashMap<String, GraphQLArgument>() {{
        put("limit", new GraphQLArgument("limit", GraphQLInt));
        put("offset", new GraphQLArgument("offset", GraphQLInt));
        put("lang", new GraphQLArgument("lang", GraphQLString));
    }};

    private List<GraphQLArgument> queryArgs = new ArrayList<GraphQLArgument>() {{
        add(defaultArguments.get("limit"));
        add(defaultArguments.get("offset"));
    }};

    private List<GraphQLArgument> nonQueryArgs = new ArrayList<GraphQLArgument>() {{
    }};

    public static HGQLSchemaConfig getInstance() {
        if(instance == null) {
            instance = new HGQLSchemaConfig();
        }
        return instance;
    }

    private void generateServices() {

        ObjectMapper mapper = new ObjectMapper();

        String packageName = "org.hypergraphql.datafetching.services";

        context.get("service").elements().forEachRemaining(service -> {
                    try {
                        String type = service.get("@type").asText();

                        Class serviceType = Class.forName(packageName + "." + type);
                        Service serviceConfig = (Service) serviceType.getConstructors()[0].newInstance();

                        serviceConfig.setParameters(service);

                        this.services.put(serviceConfig.getId(), serviceConfig);
                    } catch (IllegalAccessException e) {
                        logger.error(e);
                    } catch (InstantiationException e) {
                        logger.error(e);
                    } catch (ClassNotFoundException e) {
                        logger.error(e);
                    } catch (InvocationTargetException e) {
                        logger.error(e);
                    }
                }
        );
    }

    public void init() {
//        HGQLSchemaConfig wiring = new HGQLSchemaConfig();
//        this.schema = wiring.schema();
//        this.graphql = GraphQL.newGraphQL(this.schema).build();
    }


    public HGQLSchemaConfig(HGQLConfig config) {

        generateServices(mapper);

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
        List<RDFNode> subjects = client.getRootResources(config.HGQL_QUERY_URI, config.HGQL_QUERY_PREFIX + predicate);
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
            .description("The rdf:type of this resource (used as a filter when fetching data from its original source).")
            .dataFetcher(typeFetcher).build();


    public GraphQLObjectType registerGraphQLType(JsonNode type) {

        JsonNode fields = type.get("fields");

        String typeName = type.get("name").asText();

        Boolean isQueryType = typeName.equals("Query");

        String description;

        if (config.types().containsKey(typeName)) {
            String uri = config.types().get(typeName).id();
            description =  "Instances of \"" + uri + "\".";
        } else {
            description = (isQueryType) ? "Top querable predicates." : "An auxiliary type, not mapped to RDF.";
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
        String sourceId = (isQueryType) ? config.queryFields().get(fieldName).service().getId() : config.fields().get(fieldName).service().getId();
        String uri = (isQueryType) ? "Instances of " + fieldDef.get("targetName"): "Values of \"" + config.fields().get(fieldName).id() + "\"";
        String description = uri + " (source: "+ sourceId +").";


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
