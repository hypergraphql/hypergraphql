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
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hypergraphql.config.schema.FieldOfTypeConfig;
import org.hypergraphql.config.schema.TypeConfig;
import org.hypergraphql.config.system.ServiceConfig;
import org.hypergraphql.datafetching.services.Service;
import org.hypergraphql.exception.HGQLConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class HGQLSchemaWiring { // TODO - Remove cs suppressions

    private static final Logger LOGGER = LoggerFactory.getLogger(HGQLSchemaWiring.class);

    private final HGQLSchema hgqlSchema;
    private final GraphQLSchema schema;

    // TODO - should this be hardcoded?
    private final Map<String, GraphQLArgument> defaultArguments = Map.of(
            "limit", new GraphQLArgument("limit", GraphQLInt),
            "offset", new GraphQLArgument("offset", GraphQLInt),
            "lang", new GraphQLArgument("lang", GraphQLString),
            "uris", new GraphQLArgument("uris", new GraphQLNonNull(new GraphQLList(GraphQLID)))
    );

    private final List<GraphQLArgument> getQueryArgs = List.of(
            defaultArguments.get("limit"),
            defaultArguments.get("offset")
    );

    private final List<GraphQLArgument> getByIdQueryArgs = List.of(defaultArguments.get("uris"));

    @SuppressWarnings("checkstyle:IllegalCatch")
    public HGQLSchemaWiring(final TypeDefinitionRegistry registry,
                            final String schemaName,
                            final List<ServiceConfig> serviceConfigs) {

        try {
            this.hgqlSchema = new HGQLSchema(registry, schemaName, generateServices(serviceConfigs));
            this.schema = generateSchema();

        } catch (Exception e) {
            throw new HGQLConfigurationException("Unable to perform schema wiring", e);
        }
    }

    public GraphQLSchema getSchema() {
        return schema;
    }

    public HGQLSchema getHgqlSchema() {
        return hgqlSchema;
    }

    // TODO - investigate alternatives
    private Map<String, Service> generateServices(final List<ServiceConfig> serviceConfigs) {

        final Map<String, Service> services = new HashMap<>();
        final var packageName = "org.hypergraphql.datafetching.services";

        serviceConfigs.forEach(serviceConfig -> {
            try {
                final var type = serviceConfig.getType();

                final var serviceType = Class.forName(packageName + "." + type);
                final var service = (Service) serviceType.getConstructors()[0].newInstance();

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

        final Set<String> typeNames = this.hgqlSchema.getTypes().keySet();
        final var builtQueryType = registerGraphQLQueryType(this.hgqlSchema.getTypes().get("Query"));
        final Set<GraphQLType> builtTypes = typeNames.stream()
                .filter(typeName -> !typeName.equals("Query"))
                .map(typeName -> registerGraphQLType(this.hgqlSchema.getTypes().get(typeName)))
                .collect(Collectors.toSet());

        return GraphQLSchema.newSchema()
                .query(builtQueryType)
                .build(builtTypes);

    }

    private GraphQLFieldDefinition getIdField() {
        final var fetcherFactory = new FetcherFactory(hgqlSchema);

        return newFieldDefinition()
                .type(GraphQLID)
                .name("_id")
                .description("The URI of this resource.")
                .dataFetcher(fetcherFactory.idFetcher()).build();
    }

    private GraphQLFieldDefinition getTypeField() {
        final var fetcherFactory = new FetcherFactory(hgqlSchema);

        return newFieldDefinition()
                .type(GraphQLID)
                .name("_type")
                .description("The rdf:type of this resource (used as a filter when fetching data from its original source).")
                .dataFetcher(fetcherFactory.typeFetcher(this.hgqlSchema.getTypes())).build();
    }

    private GraphQLObjectType registerGraphQLQueryType(final TypeConfig type) {

        final var typeName = type.getName();
        final var description = "Top queryable predicates. "
                + "_GET queries return all objects of a given type, possibly restricted by limit and offset values. "
                + "_GET_BY_ID queries require a set of URIs to be specified.";

        final List<GraphQLFieldDefinition> builtFields;
        final Map<String, FieldOfTypeConfig> fields = type.getFields();
        final Set<String> fieldNames = fields.keySet();
        builtFields = fieldNames.stream()
                .map(fieldName -> registerGraphQLQueryField(type.getField(fieldName)))
                .collect(Collectors.toList());

        return newObject()
                .name(typeName)
                .description(description)
                .fields(builtFields)
                .build();
    }

    private GraphQLObjectType registerGraphQLType(final TypeConfig type) {

        final var typeName = type.getName();
        final var uri = this.hgqlSchema.getTypes().get(typeName).getId();
        final var description = "Instances of \"" + uri + "\".";
        final List<GraphQLFieldDefinition> builtFields;
        final Map<String, FieldOfTypeConfig> fields = type.getFields();
        final Set<String> fieldNames = fields.keySet();
        builtFields = fieldNames.stream()
                .map(fieldName -> registerGraphQLField(type.getField(fieldName)))
                .collect(Collectors.toList());

        builtFields.add(getIdField());
        builtFields.add(getTypeField());

        return newObject()
                .name(typeName)
                .description(description)
                .fields(builtFields)
                .build();
    }

    private GraphQLFieldDefinition registerGraphQLField(final FieldOfTypeConfig field) {
        final var fetcherFactory = new FetcherFactory(hgqlSchema);
        final var isList = field.isList();

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

    private GraphQLFieldDefinition registerGraphQLQueryField(final FieldOfTypeConfig field) {
        final var fetcherFactory = new FetcherFactory(hgqlSchema);
        return getBuiltQueryField(field, fetcherFactory.instancesOfTypeFetcher());
    }

    private GraphQLFieldDefinition getBuiltField(final FieldOfTypeConfig field, final DataFetcher fetcher) {

        final List<GraphQLArgument> args = new ArrayList<>();

        if (field.getTargetName().equals("String")) {
            args.add(defaultArguments.get("lang"));
        }

        if (field.getService() == null) {
            throw new HGQLConfigurationException("Value of 'service' for field '" + field.getName() + "' cannot be null");
        }

        final var description = field.getId() + " (source: " + field.getService().getId() + ").";

        return newFieldDefinition()
                .name(field.getName())
                .argument(args)
                .description(description)
                .type(field.getGraphqlOutputType())
                .dataFetcher(fetcher)
                .build();
    }

    private GraphQLFieldDefinition getBuiltQueryField(final FieldOfTypeConfig field, final DataFetcher fetcher) {

        final List<GraphQLArgument> args = new ArrayList<>();

        if (this.hgqlSchema.getQueryFields().get(field.getName()).type().equals(HGQL_QUERY_GET_FIELD)) {
            args.addAll(getQueryArgs);
        } else {
            args.addAll(getByIdQueryArgs);
        }

        final var queryFieldConfig = this.hgqlSchema.getQueryFields().get(field.getName());

        final var service = queryFieldConfig.service();
        if (service == null) {
            throw new HGQLConfigurationException("Service for field '" + field.getName() + "':['"
                    + queryFieldConfig.type() + "'] not specified (null)");
        }
        final var serviceId = service.getId();
        final var description = (queryFieldConfig.type().equals(HGQL_QUERY_GET_FIELD))
                ? "Get instances of " + field.getTargetName() + " (service: " + serviceId + ")"
                : "Get instances of " + field.getTargetName() + " by URIs (service: " + serviceId + ")";

        return newFieldDefinition()
                .name(field.getName())
                .argument(args)
                .description(description)
                .type(field.getGraphqlOutputType())
                .dataFetcher(fetcher)
                .build();
    }

}
