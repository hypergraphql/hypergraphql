package org.hypergraphql.config.system;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;
import org.hypergraphql.datamodel.HGQLSchema;
import org.hypergraphql.datamodel.HGQLSchemaWiring;
import org.hypergraphql.exception.HGQLConfigurationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
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
    private HGQLConfig(
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

    public static HGQLConfig fromClasspathConfig(final String path) {

        final InputStream in = HGQLConfig.class.getClassLoader().getResourceAsStream(path);
        return new HGQLConfig(in);
    }

    public static HGQLConfig fromFileSystemPath(final String path) {

        try {
            return new HGQLConfig(new FileInputStream(path));
        } catch (FileNotFoundException e) {
            throw new HGQLConfigurationException("Unable to find config file", e);
        }
    }

    public static HGQLConfig from(final InputStream inputStream) {
        return new HGQLConfig(inputStream);
    }

    private HGQLConfig(final InputStream inputStream) {

        ObjectMapper mapper = new ObjectMapper();

        try {
            HGQLConfig config = mapper.readValue(inputStream, HGQLConfig.class);

            SchemaParser schemaParser = new SchemaParser();
            this.registry = schemaParser.parse(new File(config.schemaFile));

            this.name = config.name;
            this.schemaFile = config.schemaFile;
            this.graphqlConfig = config.graphqlConfig;
            checkServicePorts(config.serviceConfigs);
            this.serviceConfigs = config.serviceConfigs;
            HGQLSchemaWiring wiring = new HGQLSchemaWiring(this.registry, this.name, this.serviceConfigs);
            this.schema = wiring.getSchema();
            this.hgqlSchema = wiring.getHgqlSchema();
        } catch (IOException e) {
            throw new HGQLConfigurationException("Error reading from configuration file", e);
        }
    }

    public GraphqlConfig getGraphqlConfig() {
        return graphqlConfig;
    }

    public String getName() {
        return name;
    }

    @JsonIgnore
    private void checkServicePorts(final List<ServiceConfig> serviceConfigs) {

        serviceConfigs.forEach(serviceConfig -> {
            try {
                if(serviceConfig.getUrl() != null) {
                    final URI serviceUri = new URI(serviceConfig.getUrl());
                    if (serviceUri.getHost().equals("localhost") && serviceUri.getPort() <= 0) {
                        final URIBuilder uriBuilder = new URIBuilder(serviceUri);
                        uriBuilder.setPort(graphqlConfig.port());
                        serviceConfig.setUrl(uriBuilder.build().toString());
                    }
                }
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        });
    }
}


