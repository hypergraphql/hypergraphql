package org.hypergraphql.datamodel;

import static graphql.Scalars.GraphQLID;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_QUERY_GET_FIELD;
import static org.hypergraphql.config.schema.HGQLVocabulary.SCALAR_TYPES;

import graphql.language.Field;
import graphql.schema.*;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.log4j.Logger;
import org.hypergraphql.config.schema.*;
import org.hypergraphql.config.system.FetchParams;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.config.system.ServiceConfig;
import org.hypergraphql.datafetching.services.Service;

/**
 * Created by szymon on 24/08/2017.
 * <p>
 * This class defines the GraphQL wiring (data fetchers)
 */

public class HGQLSchemaWiring {

    static Logger logger = Logger.getLogger(HGQLSchemaWiring.class);


    public GraphQLSchema getSchema() {

        return schema;
    }

    public Map<String, TypeConfig> getTypes() {
        return types;
    }

    public Map<String, FieldConfig> getFields() {
        return fields;
    }

    public Map<String, QueryFieldConfig> getQueryFields() {
        return queryFields;
    }

    private static HGQLSchemaWiring instance = null;

    private GraphQLSchema schema;
    private HGQLSchema hgqlSchema;
    private Map<String, Service> services;

    private Map<String, TypeConfig> types;
    private Map<String, FieldConfig> fields;
    private Map<String, QueryFieldConfig> queryFields;

    public String getRdfSchemaOutput(String format) {

        return hgqlSchema.getRdfSchemaOutput(format);

    }

    private Map<String, GraphQLArgument> defaultArguments = new HashMap<String, GraphQLArgument>() {{
        put("limit", new GraphQLArgument("limit", GraphQLInt));
        put("offset", new GraphQLArgument("offset", GraphQLInt));
        put("lang", new GraphQLArgument("lang", GraphQLString));
        put("uris", new GraphQLArgument("uris", new GraphQLList(GraphQLID)));
    }};

    private List<GraphQLArgument> getQueryArgs = new ArrayList<GraphQLArgument>() {{
        add(defaultArguments.get("limit"));
        add(defaultArguments.get("offset"));
    }};

    private List<GraphQLArgument> getByIdQueryArgs = new ArrayList<GraphQLArgument>() {{
        add(defaultArguments.get("uris"));
    }};

    public static HGQLSchemaWiring getInstance() {
        return instance;
    }

    public static HGQLSchemaWiring build(HGQLConfig config) {

        if(instance == null) {
            instance = new HGQLSchemaWiring(config);
        }
        return instance;
    }

    public HGQLSchemaWiring(HGQLConfig config) {

        this.services = new HashMap<>();

        generateServices(config.getServiceConfigs());

        try {
            this.hgqlSchema = new HGQLSchema(config.getRegistry(), config.getName(), this.services);

        } catch (Exception e) {
            e.printStackTrace();
        }

        this.fields = hgqlSchema.getFields();
        this.types = hgqlSchema.getTypes();
        this.queryFields = hgqlSchema.getQueryFields();
        this.schema = generateSchema();
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

    private GraphQLSchema generateSchema() {

        Set<String> typeNames = types.keySet();
        GraphQLObjectType builtQueryType = registerGraphQLQueryType(types.get("Query"));
        Set<GraphQLType> builtTypes = new HashSet<>();

        for (String typeName : typeNames) {
            if (!typeName.equals("Query")) {
                builtTypes.add(registerGraphQLType(types.get(typeName)));
            }
        }

        return GraphQLSchema.newSchema()
                .query(builtQueryType)
                .build(builtTypes);

    }

    private DataFetcher<String> idFetcher = environment -> {
        RDFNode thisNode = environment.getSource();

        if (thisNode.asResource().isURIResource()) {

            return thisNode.asResource().getURI();

        } else {
            return "_:" + thisNode.asNode().getBlankNodeLabel();
        }
    };

    private DataFetcher<String> typeFetcher = environment -> {
        String typeName = environment.getParentType().getName();
        String type = (this.getTypes().containsKey(typeName))? getTypes().get(typeName).getId() : null;
        return type;

    };

    private DataFetcher<List<RDFNode>> instancesOfTypeFetcher = environment -> {
        Field field = (Field) environment.getFields().toArray()[0];
        String predicate = (field.getAlias()!=null) ? field.getAlias() : field.getName();
        ModelContainer client = environment.getContext();
        List<RDFNode> subjects = client.getValuesOfObjectProperty(HGQLVocabulary.HGQL_QUERY_URI, HGQLVocabulary.HGQL_QUERY_NAMESPACE + predicate);
        return subjects;
    };

    private DataFetcher<List<RDFNode>> objectsFetcher = environment -> {

        FetchParams params = new FetchParams(environment);
        return params.getClient().getValuesOfObjectProperty(params.getSubjectResource(), params.getPredicateURI());
    };

    private DataFetcher<RDFNode> objectFetcher = environment -> {

        FetchParams params = new FetchParams(environment);
        return params.getClient().getValueOfObjectProperty(params.getSubjectResource(), params.getPredicateURI());
    };

    private DataFetcher<List<String>> literalValuesFetcher = environment -> {

        FetchParams params = new FetchParams(environment);
        return params.getClient().getValuesOfDataProperty(params.getSubjectResource(), params.getPredicateURI(), environment.getArguments());
    };

    private DataFetcher<String> literalValueFetcher = environment -> {

        FetchParams params = new FetchParams(environment);
        return params.getClient().getValueOfDataProperty(params.getSubjectResource(), params.getPredicateURI(), environment.getArguments());
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


    private GraphQLObjectType registerGraphQLQueryType(TypeConfig type) {

        String typeName = type.getName();
        String description =  "Top querable predicates. " +
                "_GET queries return all objects of a given type, possibly restricted by limit and offset values. " +
                "_GET_BY_ID queries require a set of URIs to be specified.";

        List<GraphQLFieldDefinition> builtFields = new ArrayList<>();

        Map<String, FieldOfTypeConfig> fields = type.getFields();

        Set<String> fieldNames = fields.keySet();

        for (String fieldName : fieldNames) {
            builtFields.add(registerGraphQLQueryField(type.getField(fieldName)));
        }

        GraphQLObjectType newObjectType = newObject()
                .name(typeName)
                .description(description)
                .fields(builtFields)
                .build();

        return newObjectType;
    }

    private GraphQLObjectType registerGraphQLType(TypeConfig type) {

        String typeName = type.getName();
        String uri = getTypes().get(typeName).getId();
        String description =  "Instances of \"" + uri + "\".";

        List<GraphQLFieldDefinition> builtFields = new ArrayList<>();

        Map<String, FieldOfTypeConfig> fields = type.getFields();

        Set<String> fieldNames = fields.keySet();

        for (String fieldName : fieldNames) {
            builtFields.add(registerGraphQLField(type.getField(fieldName)));
        }

        builtFields.add(_idField);
        builtFields.add(_typeField);

        GraphQLObjectType newObjectType = newObject()
                .name(typeName)
                .description(description)
                .fields(builtFields)
                .build();

        return newObjectType;
    }

    private GraphQLFieldDefinition registerGraphQLField(FieldOfTypeConfig field) {

            if (SCALAR_TYPES.containsKey(field.getTargetName())) {
                return getBuiltField(field, literalFetchers.get(field.getIsList()));
            } else {
                return getBuiltField(field, objectFetchers.get(field.getIsList()));
        }

    }

    private GraphQLFieldDefinition registerGraphQLQueryField(FieldOfTypeConfig field) {

        return getBuiltQueryField(field, instancesOfTypeFetcher);
    }

    private GraphQLFieldDefinition getBuiltField(FieldOfTypeConfig field, DataFetcher fetcher) {

        List<GraphQLArgument> args = new ArrayList<>();

        if (field.getTargetName().equals("String")) {
                args.add(defaultArguments.get("lang"));
        }

        String description = field.getId() + " (source: "+ field.getService().getId() +").";

        GraphQLFieldDefinition builtField = newFieldDefinition()
                .name(field.getName())
                .argument(args)
                .description(description)
                .type(field.getGraphqlOutputType())
                .dataFetcher(fetcher).build();

        return builtField;

    }

    private GraphQLFieldDefinition getBuiltQueryField(FieldOfTypeConfig field, DataFetcher fetcher) {

        List<GraphQLArgument> args = new ArrayList<>();

        if (queryFields.get(field.getName()).type().equals(HGQL_QUERY_GET_FIELD)) {
            args.addAll(getQueryArgs);
        } else {
            args.addAll(getByIdQueryArgs);
        }

        String description = (queryFields.get(field.getName()).type().equals(HGQL_QUERY_GET_FIELD)) ?
                "Get instances of " + field.getTargetName() + "." : "Get instances of " + field.getTargetName() + " by URIs.";

        GraphQLFieldDefinition builtField = newFieldDefinition()
                .name(field.getName())
                .argument(args)
                .description(description)
                .type(field.getGraphqlOutputType())
                .dataFetcher(fetcher).build();

        return builtField;

    }

}
