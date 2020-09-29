package org.hypergraphql.config.system;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import graphql.schema.GraphQLSchema;
import org.hypergraphql.datamodel.HGQLSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by szymon on 05/09/2017.
 */

public class HGQLConfig {

    private final String name;
    private final String schemaFile;
    private final GraphqlConfig graphqlConfig;
    private final List<ServiceConfig> serviceConfigs;

    private GraphQLSchema schema;
    private HGQLSchema hgqlSchema;

    @JsonCreator
    private HGQLConfig(
            @JsonProperty("name") final String name,
            @JsonProperty("schema") final String schemaFile,
            @JsonProperty("server") final GraphqlConfig graphqlConfig,
            @JsonProperty("services") final List<ServiceConfig> services
    ) {
        this.name = name;
        this.schemaFile = schemaFile;
        this.graphqlConfig = graphqlConfig;
        this.serviceConfigs = services;
    }
//    public void setName(String name) {
//        this.name = name;
//    }

    public GraphQLSchema getSchema() {
        return schema;
    }

    public void setSchema(GraphQLSchema schema) {
        this.schema = schema;
    }

    public HGQLSchema getHgqlSchema() {
        return hgqlSchema;
    }

    public GraphqlConfig getGraphqlConfig() {
        return graphqlConfig;
    }

    public String getName() {
        return name;
    }

    @JsonIgnore
    public String getSchemaFile() {
        return schemaFile;
    }

    @JsonIgnore
    public List<ServiceConfig> getServiceConfigs() {
        return serviceConfigs;
    }

    @JsonIgnore
    public void setGraphQLSchema(final GraphQLSchema schema) {
        this.schema = schema;
    }

    @JsonIgnore
    public void setHgqlSchema(final HGQLSchema schema) {
        this.hgqlSchema = schema;
    }
}


