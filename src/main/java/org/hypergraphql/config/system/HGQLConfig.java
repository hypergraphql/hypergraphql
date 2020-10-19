package org.hypergraphql.config.system;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import graphql.schema.GraphQLSchema;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hypergraphql.datamodel.HGQLSchema;

/**
 * Created by szymon on 05/09/2017.
 */
@Getter
@Setter
public final class HGQLConfig {

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

    @JsonIgnore
    public String getSchemaFile() {
        return schemaFile;
    }

    @JsonIgnore
    public List<ServiceConfig> getServiceConfigs() {
        return serviceConfigs;
    }

    @JsonIgnore
    public void setGraphQLSchema(final GraphQLSchema graphQLSchema) {
        this.schema = graphQLSchema;
    }

    @JsonIgnore
    public void setHgqlSchema(final HGQLSchema hgqlSchema) {
        this.hgqlSchema = hgqlSchema;
    }
}


