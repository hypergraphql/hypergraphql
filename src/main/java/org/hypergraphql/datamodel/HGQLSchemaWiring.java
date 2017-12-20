package org.hypergraphql.datamodel;

import static graphql.Scalars.GraphQLID;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_QUERY_GET_FIELD;
import static org.hypergraphql.config.schema.HGQLVocabulary.SCALAR_TYPES;

import graphql.schema.*;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import graphql.schema.idl.TypeDefinitionRegistry;

import org.apache.log4j.Logger;
import org.hypergraphql.config.schema.*;

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


    private GraphQLSchema schema;

    public HGQLSchema getHgqlSchema() {
        return hgqlSchema;
    }

    private HGQLSchema hgqlSchema;


    public String getRdfSchemaOutput(String format) {

        return hgqlSchema.getRdfSchemaOutput(format);

    }

    private Map<String, GraphQLArgument> defaultArguments = new HashMap<String, GraphQLArgument>() {{
        put("limit", new GraphQLArgument("limit", GraphQLInt));
        put("offset", new GraphQLArgument("offset", GraphQLInt));
        put("lang", new GraphQLArgument("lang", GraphQLString));
        put("uris", new GraphQLArgument("uris", new GraphQLNonNull(new GraphQLList(GraphQLID))));
    }};

    private List<GraphQLArgument> getQueryArgs = new ArrayList<GraphQLArgument>() {{
        add(defaultArguments.get("limit"));
        add(defaultArguments.get("offset"));
    }};

    private List<GraphQLArgument> getByIdQueryArgs = new ArrayList<GraphQLArgument>() {{
        add(defaultArguments.get("uris"));
    }};


    public HGQLSchemaWiring(TypeDefinitionRegistry registry, String schemaName, List<ServiceConfig> serviceConfigs) {


        try {
            this.hgqlSchema = new HGQLSchema(registry, schemaName, generateServices(serviceConfigs));
            this.schema = generateSchema();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map<String, Service> generateServices(List<ServiceConfig> serviceConfigs) {

        Map<String, Service> services = new HashMap<>();

        String packageName = "org.hypergraphql.datafetching.services";

        for (ServiceConfig serviceConfig : serviceConfigs) {
            try {
                String type = serviceConfig.getType();

                Class serviceType = Class.forName(packageName + "." + type);
                Service service = (Service) serviceType.getConstructors()[0].newInstance();

                service.setParameters(serviceConfig);


                services.put(serviceConfig.getId(), service);
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

        return services;
    }

    private GraphQLSchema generateSchema() {

        Set<String> typeNames = this.hgqlSchema.getTypes().keySet();
        GraphQLObjectType builtQueryType = registerGraphQLQueryType(this.hgqlSchema.getTypes().get("Query"));
        Set<GraphQLType> builtTypes = new HashSet<>();

        for (String typeName : typeNames) {
            if (!typeName.equals("Query")) {
                builtTypes.add(registerGraphQLType(this.hgqlSchema.getTypes().get(typeName)));
            }
        }

        return GraphQLSchema.newSchema()
                .query(builtQueryType)
                .build(builtTypes);

}



    private GraphQLFieldDefinition getidField() {
        FetcherFactory fetcherFactory = new FetcherFactory(hgqlSchema);

       return  newFieldDefinition()
                .type(GraphQLID)
                .name("_id")
                .description("The URI of this resource.")
                .dataFetcher(fetcherFactory.idFetcher()).build();
    }

    private GraphQLFieldDefinition gettypeField() {
    FetcherFactory fetcherFactory = new FetcherFactory(hgqlSchema);

        return newFieldDefinition()
                .type(GraphQLID)
                .name("_type")
                .description("The rdf:type of this resource (used as a filter when fetching data from its original source).")
                .dataFetcher(fetcherFactory.typeFetcher(this.hgqlSchema.getTypes())).build();
    }


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
        String uri = this.hgqlSchema.getTypes().get(typeName).getId();
        String description =  "Instances of \"" + uri + "\".";

        List<GraphQLFieldDefinition> builtFields = new ArrayList<>();

        Map<String, FieldOfTypeConfig> fields = type.getFields();

        Set<String> fieldNames = fields.keySet();

        for (String fieldName : fieldNames) {
            builtFields.add(registerGraphQLField(type.getField(fieldName)));
        }

        builtFields.add(getidField());
        builtFields.add(gettypeField());

        GraphQLObjectType newObjectType = newObject()
                .name(typeName)
                .description(description)
                .fields(builtFields)
                .build();

        return newObjectType;
    }

    private GraphQLFieldDefinition registerGraphQLField(FieldOfTypeConfig field) {
        FetcherFactory fetcherFactory = new FetcherFactory(hgqlSchema);

        Boolean isList = field.getIsList();

            if (SCALAR_TYPES.containsKey(field.getTargetName())) {
                if (isList) return getBuiltField(field,fetcherFactory.literalValuesFetcher());
                else return getBuiltField(field,fetcherFactory.literalValueFetcher());

            } else {
                if (isList) return getBuiltField(field,fetcherFactory.objectsFetcher());
                else return getBuiltField(field,fetcherFactory.objectFetcher());

        }

    }

    private GraphQLFieldDefinition registerGraphQLQueryField(FieldOfTypeConfig field) {
        FetcherFactory fetcherFactory = new FetcherFactory(hgqlSchema);

        return getBuiltQueryField(field, fetcherFactory.instancesOfTypeFetcher());
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

        if (this.hgqlSchema.getQueryFields().get(field.getName()).type().equals(HGQL_QUERY_GET_FIELD)) {
            args.addAll(getQueryArgs);
        } else {
            args.addAll(getByIdQueryArgs);
        }

        String serviceId = this.hgqlSchema.getQueryFields().get(field.getName()).service().getId();
        String description = (this.hgqlSchema.getQueryFields().get(field.getName()).type().equals(HGQL_QUERY_GET_FIELD)) ?
                "Get instances of " + field.getTargetName() + " (service: " + serviceId + ")"  : "Get instances of " + field.getTargetName() + " by URIs (service: " + serviceId + ")";

        GraphQLFieldDefinition builtField = newFieldDefinition()
                .name(field.getName())
                .argument(args)
                .description(description)
                .type(field.getGraphqlOutputType())
                .dataFetcher(fetcher).build();

        return builtField;

    }

}
