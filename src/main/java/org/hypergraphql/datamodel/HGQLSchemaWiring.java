package org.hypergraphql.datamodel;

import graphql.schema.DataFetcher;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.hypergraphql.config.schema.FieldOfTypeConfig;
import org.hypergraphql.config.schema.QueryFieldConfig;
import org.hypergraphql.config.schema.TypeConfig;
import org.hypergraphql.config.system.ServiceConfig;
import org.hypergraphql.datafetching.services.Service;
import org.hypergraphql.exception.HGQLConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static graphql.Scalars.GraphQLID;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_QUERY_GET_FIELD;
import static org.hypergraphql.config.schema.HGQLVocabulary.SCALAR_TYPES;

/**
 * Created by szymon on 24/08/2017.
 * <p>
 * This class defines the GraphQL wiring (data fetchers)
 */

public class HGQLSchemaWiring {

    private static final Logger LOGGER = LoggerFactory.getLogger(HGQLSchemaWiring.class);

    private HGQLSchema hgqlSchema;
    private GraphQLSchema schema;

    public GraphQLSchema getSchema() {

        return schema;
    }

    public HGQLSchema getHgqlSchema() {
        return hgqlSchema;
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
            throw new HGQLConfigurationException("Unable to perform schema wiring", e);
        }
    }

    private Map<String, Service> generateServices(List<ServiceConfig> serviceConfigs) {

        Map<String, Service> services = new HashMap<>();

        String packageName = "org.hypergraphql.datafetching.services";

        serviceConfigs.forEach(serviceConfig -> {
            try {
                String type = serviceConfig.getType();

                Class serviceType = Class.forName(packageName + "." + type);
                Service service = (Service) serviceType.getConstructors()[0].newInstance();

                service.setParameters(serviceConfig);

                services.put(serviceConfig.getId(), service);
            } catch (IllegalAccessException
                    | InstantiationException
                    | ClassNotFoundException
                    | InvocationTargetException e) {
                LOGGER.error("Problem adding service {}", serviceConfig.getId(), e);
                throw new HGQLConfigurationException("Error wiring up services", e);
            }
        });

        return services;
    }

    private GraphQLSchema generateSchema() {

        Set<String> typeNames = this.hgqlSchema.getTypes().keySet();
        GraphQLObjectType builtQueryType = registerGraphQLQueryType(this.hgqlSchema.getTypes().get("Query"));
        Set<GraphQLType> builtTypes = typeNames.stream()
                .filter(typeName -> !typeName.equals("Query"))
                .map(typeName -> registerGraphQLType(this.hgqlSchema.getTypes().get(typeName)))
                .collect(Collectors.toSet());

        return GraphQLSchema.newSchema()
                .query(builtQueryType)
                .build(builtTypes);

    }


    private GraphQLFieldDefinition getidField() {
        FetcherFactory fetcherFactory = new FetcherFactory(hgqlSchema);

        return newFieldDefinition()
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
        String description = "Top queryable predicates. " +
                "_GET queries return all objects of a given type, possibly restricted by limit and offset values. " +
                "_GET_BY_ID queries require a set of URIs to be specified.";

        List<GraphQLFieldDefinition> builtFields;

        Map<String, FieldOfTypeConfig> fields = type.getFields();

        Set<String> fieldNames = fields.keySet();

        builtFields = fieldNames.stream()
                .map(fieldName -> registerGraphQLQueryField(type.getField(fieldName)))
                .collect(Collectors.toList());

        return newObject()
                .name(typeName)
                .description(description)
                .fields(builtFields)
                .build();
    }

    private GraphQLObjectType registerGraphQLType(TypeConfig type) {

        String typeName = type.getName();
        String uri = this.hgqlSchema.getTypes().get(typeName).getId();
        String description = "Instances of \"" + uri + "\".";

        List<GraphQLFieldDefinition> builtFields;

        Map<String, FieldOfTypeConfig> fields = type.getFields();

        Set<String> fieldNames = fields.keySet();

        builtFields = fieldNames.stream()
                .map(fieldName -> registerGraphQLField(type.getField(fieldName)))
                .collect(Collectors.toList());

        builtFields.add(getidField());
        builtFields.add(gettypeField());

        return newObject()
                .name(typeName)
                .description(description)
                .fields(builtFields)
                .build();
    }

    private GraphQLFieldDefinition registerGraphQLField(FieldOfTypeConfig field) {
        FetcherFactory fetcherFactory = new FetcherFactory(hgqlSchema);

        Boolean isList = field.isList();

        if (SCALAR_TYPES.containsKey(field.getTargetName())) {
            if (isList) {
                return getBuiltField(field, fetcherFactory.literalValuesFetcher());
            } else {
                return getBuiltField(field, fetcherFactory.literalValueFetcher());
            }

        } else {
            if (isList) {
                return getBuiltField(field, fetcherFactory.objectsFetcher());
            } else {
                return getBuiltField(field, fetcherFactory.objectFetcher());
            }
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

        if(field.getService() == null) {
            throw new HGQLConfigurationException("Value of 'service' for field '" + field.getName() + "' cannot be null");
        }

        String description = field.getId() + " (source: " + field.getService().getId() + ").";

        return newFieldDefinition()
                .name(field.getName())
                .argument(args)
                .description(description)
                .type(field.getGraphqlOutputType())
                .dataFetcher(fetcher)
                .build();
    }

    private GraphQLFieldDefinition getBuiltQueryField(FieldOfTypeConfig field, DataFetcher fetcher) {

        List<GraphQLArgument> args = new ArrayList<>();

        if (this.hgqlSchema.getQueryFields().get(field.getName()).type().equals(HGQL_QUERY_GET_FIELD)) {
            args.addAll(getQueryArgs);
        } else {
            args.addAll(getByIdQueryArgs);
        }

        final QueryFieldConfig queryFieldConfig = this.hgqlSchema.getQueryFields().get(field.getName());

        Service service = queryFieldConfig.service();
        if(service == null) {
            throw new HGQLConfigurationException("Service for field '" + field.getName() + "':['"
                    + queryFieldConfig.type() + "'] not specified (null)");
        }
        String serviceId = service.getId();
        String description = (queryFieldConfig.type().equals(HGQL_QUERY_GET_FIELD)) ?
                "Get instances of " + field.getTargetName() + " (service: " + serviceId + ")" :
                "Get instances of " + field.getTargetName() + " by URIs (service: " + serviceId + ")";

        return newFieldDefinition()
                .name(field.getName())
                .argument(args)
                .description(description)
                .type(field.getGraphqlOutputType())
                .dataFetcher(fetcher)
                .build();
    }

}
