package org.hypergraphql.config.system;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.apache.log4j.Logger;
import org.hypergraphql.datamodel.HGQLSchema;
import org.hypergraphql.datamodel.HGQLSchemaWiring;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by szymon on 05/09/2017.
 */

public class HGQLConfig {

    private String name;
    private String schemaFile;
    private GraphqlConfig graphqlConfig;
    private List<ServiceConfig> serviceConfigs;
    private TypeDefinitionRegistry registry;

    public void setName(String name) {
        this.name = name;
    }

    public GraphQLSchema getSchema() {
        return schema;
    }

    public void setSchema(GraphQLSchema schema) {
        this.schema = schema;
    }

    public HGQLSchema getHgqlSchema() {
        return hgqlSchema;
    }

    public static Logger getLogger() {
        return logger;
    }

    public static void setLogger(Logger logger) {
        HGQLConfig.logger = logger;
    }

    private GraphQLSchema schema;
    private HGQLSchema hgqlSchema;

    private static Logger logger = Logger.getLogger(HGQLConfig.class);

    @JsonCreator
    private  HGQLConfig(
            @JsonProperty("name") String name,
            @JsonProperty("schema") String schemaFile,
            @JsonProperty("server") GraphqlConfig graphqlConfig,
            @JsonProperty("services") List<ServiceConfig> services
    ) {
        this.name = name;
        this.schemaFile = schemaFile;
        this.graphqlConfig = graphqlConfig;
        this.serviceConfigs = services;
    }

    public HGQLConfig(String propertyFilepath) {

        ObjectMapper mapper = new ObjectMapper();

        try {
            HGQLConfig config = mapper.readValue(new File(propertyFilepath), HGQLConfig.class);

            SchemaParser schemaParser = new SchemaParser();
            this.registry = schemaParser.parse(new File(config.schemaFile));

            this.name = config.name;
            this.schemaFile = config.schemaFile;
            this.serviceConfigs = config.serviceConfigs;
            this.graphqlConfig = config.graphqlConfig;
            HGQLSchemaWiring wiring = new HGQLSchemaWiring(this.registry, this.name, this.serviceConfigs);
            this.schema = wiring.getSchema();
            this.hgqlSchema = wiring.getHgqlSchema();

        } catch (IOException e) {
            logger.error(e);
        }


    }

    public GraphqlConfig getGraphqlConfig() {
        return graphqlConfig;
    }

    public String getName() {
        return name;
    }


    public TypeDefinitionRegistry getRegistry() {
        return registry;
    }

    public String getSchemaFile() {
        return schemaFile;
    }
}


