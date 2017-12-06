package org.hypergraphql.config.schema;

import static graphql.Scalars.GraphQLID;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.language.Field;
import graphql.schema.*;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.log4j.Logger;
import org.hypergraphql.config.system.FetchParams;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.config.system.ServiceConfig;
import org.hypergraphql.datafetching.services.Service;
import org.hypergraphql.datamodel.HGQLSchema;
import org.hypergraphql.datamodel.ModelContainer;

/**
 * Created by szymon on 24/08/2017.
 * <p>
 * This class defines the GraphQL wiring (data fetchers and type resolvers)
 */

public class HGQLSchemaWiring {

    static Logger logger = Logger.getLogger(HGQLSchemaWiring.class);


    private static HGQLSchemaWiring instance = null;

    public GraphQLSchema getSchema() {
        return schema;
    }

    private GraphQLSchema schema;
    //private GraphQL graphql;
    private JsonNode schemaJson;
    private HGQLConfig config;
    private HGQLSchema hgqlSchema;
    private Map<String, Service> services;

    public Map<String, GraphQLOutputType> outputTypes() {
        return outputTypes;
    }

    public Map<String, TypeConfig> types() {
        return types;
    }

    public Map<String, FieldConfig> fields() {
        return fields;
    }

    public Map<String, QueryFieldConfig> queryFields() {
        return queryFields;
    }

    public ObjectNode mapping() {
        return mapping;
    }

    private ObjectNode mapping;
    private Map<String, GraphQLOutputType> outputTypes = new HashMap<>();
    private Map<String, TypeConfig> types;
    private Map<String, FieldConfig> fields;
    private Map<String, QueryFieldConfig> queryFields;

    private Map<String, GraphQLArgument> defaultArguments = new HashMap<String, GraphQLArgument>() {{
        put("limit", new GraphQLArgument("limit", GraphQLInt));
        put("offset", new GraphQLArgument("offset", GraphQLInt));
        put("lang", new GraphQLArgument("lang", GraphQLString));
        put("uris", new GraphQLArgument("offset", new GraphQLList(GraphQLString)));
    }};

    private List<GraphQLArgument> getQueryArgs = new ArrayList<GraphQLArgument>() {{
        add(defaultArguments.get("limit"));
        add(defaultArguments.get("offset"));
    }};

    private List<GraphQLArgument> getByIdQueryArgs = new ArrayList<GraphQLArgument>() {{
        add(defaultArguments.get("uris"));
    }};

//    private List<GraphQLArgument> nonQueryArgs = new ArrayList<GraphQLArgument>() {{
//    }};

    public static HGQLSchemaWiring getInstance() {
        return instance;
    }

    public static HGQLSchemaWiring build(HGQLConfig config) {

        if(instance == null) {
            instance = new HGQLSchemaWiring(config);
        }
        return instance;
    }

    private void generateServices(List<ServiceConfig> serviceConfigs) {

        String packageName = "org.hypergraphql.datafetching.services";

        for (ServiceConfig serviceConfig : serviceConfigs) {
            try {
                String type = serviceConfig.getType();

                Class serviceType = Class.forName(packageName + "." + type);
                Service service = (Service) serviceType.getConstructors()[0].newInstance();

                service.setParameters(serviceConfig);

                this.services.put(serviceConfig.getId(), service);
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
    }

  //  public void init() {
//        HGQLSchemaWiring wiring = new HGQLSchemaWiring();
//        this.schema = wiring.schema();
//        this.graphql = GraphQL.newGraphQL(this.schema).build();
 //   }


    public HGQLSchemaWiring(HGQLConfig config) {

        this.services = new HashMap<>();

        generateServices(config.getServiceConfigs());

        try {
            this.hgqlSchema = new HGQLSchema(config.getRegistry(), config.getName(), this.services);
        } catch (Exception e) {
            e.printStackTrace();
        }

//        Set<GraphQLType> types = new HashSet<>();
//        GraphQLObjectType queryType = null;
//
//        JsonNode mapping = mapping();
//
//        Iterator<String> typeNames = mapping().fieldNames();
//
//        while(typeNames.hasNext()) {
//            String typeName = typeNames.next();
//            GraphQLObjectType typeDef = registerGraphQLType(mapping().get(typeName));
//
//            if (typeName.equals("Query")) {
//                queryType = typeDef;
//            } else {
//                types.add(typeDef);
//            }
//        }
//
//        this.schema = GraphQLSchema.newSchema()
//                .query(queryType)
//                .build(types);

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
        String type = (this.types().containsKey(typeName))? types().get(typeName).id() : null;
        return type;

    };

    private DataFetcher<List<RDFNode>> instancesOfTypeFetcher = environment -> {
        Field field = (Field) environment.getFields().toArray()[0];
        String predicate = (field.getAlias()!=null) ? field.getAlias() : field.getName();
        ModelContainer client = environment.getContext();
        List<RDFNode> subjects = client.getRootResources(HGQLVocabulary.HGQL_QUERY_URI, HGQLVocabulary.HGQL_QUERY_NAMESPACE + predicate);
        return subjects;
    };

    private DataFetcher<List<RDFNode>> objectsFetcher = environment -> {

        FetchParams params = new FetchParams(environment);
        return params.getClient().getValuesOfObjectProperty(params.getSubjectResource(), params.getProperty(), environment.getArguments());
    };

    private DataFetcher<RDFNode> objectFetcher = environment -> {

        FetchParams params = new FetchParams(environment);
        return params.getClient().getValueOfObjectProperty(params.getSubjectResource(), params.getProperty(), environment.getArguments());
    };

    private DataFetcher<List<Object>> literalValuesFetcher = environment -> {

        FetchParams params = new FetchParams(environment);
        return params.getClient().getValuesOfDataProperty(params.getSubjectResource(), params.getProperty(), environment.getArguments());
    };

    private DataFetcher<Object> literalValueFetcher = environment -> {

        FetchParams params = new FetchParams(environment);
        return params.getClient().getValueOfDataProperty(params.getSubjectResource(), params.getProperty(), environment.getArguments());
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

        if (types().containsKey(typeName)) {
            String uri = types().get(typeName).id();
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

        if (types().containsKey(typeName)) builtFields.add(_typeField);

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

//        GraphQLOutputType refType = outputType(fieldDef.get("targetTypeId").asText());
//
//        List<GraphQLArgument> args = new ArrayList<>();
//
//        if (isQueryType) {
//            args.addAll(queryArgs);
//        } else {
//            args.addAll(nonQueryArgs);
//            if (fieldDef.get("targetName").asText().equals("String")) {
//                args.add(defaultArguments.get("lang"));
//            }
//        }
//
//        String fieldName = fieldDef.get("name").asText();
//        String sourceId = (isQueryType) ? queryFields().get(fieldName).service().getId() : fields().get(fieldName).service().getId();
//        String uri = (isQueryType) ? "Instances of " + fieldDef.get("targetName"): "Values of \"" + fields().get(fieldName).id() + "\"";
//        String description = uri + " (source: "+ sourceId +").";
//
//
//        GraphQLFieldDefinition field = newFieldDefinition()
//                .name(fieldName)
//                .argument(args)
//                .description(description)
//                .type(refType)
//                .dataFetcher(fetcher).build();
//
//        return field;
        return null;
    }

    public GraphQLSchema schema() {
        return schema;
    }
}
