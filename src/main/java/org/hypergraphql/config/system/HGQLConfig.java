package org.hypergraphql.config.system;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.language.TypeDefinition;
import graphql.schema.*;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.apache.log4j.Logger;

import org.hypergraphql.datafetching.services.Service;
import org.hypergraphql.config.schema.FieldConfig;
import org.hypergraphql.config.schema.QueryFieldConfig;
import org.hypergraphql.config.schema.TypeConfig;

import static graphql.Scalars.*;

/**
 * Created by szymon on 05/09/2017.
 */

public class HGQLConfig {

    private String name;
    private String serviceFile;
    private String schemaFile;
    private GraphqlConfig graphqlConfig;
    private List<ServiceConfig> serviceConfigs;
    private TypeDefinitionRegistry registry;

    private static Logger logger = Logger.getLogger(HGQLConfig.class);

    @JsonCreator
    private  HGQLConfig(
            @JsonProperty("name") String name,
            @JsonProperty("serviceFile") String serviceFile,
            @JsonProperty("schemaFile") String schemaFile,
            @JsonProperty("graphql") GraphqlConfig graphql
    ) {
        this.name = name;
        this.serviceFile = serviceFile;
        this.schemaFile = schemaFile;
        this.graphqlConfig = graphql;
    }



    public HGQLConfig(String propertyFilepath) {

        ObjectMapper mapper = new ObjectMapper();

        try {
            HGQLConfig config = mapper.readValue(new File(propertyFilepath), HGQLConfig.class);

            if (config != null) {

                try {

                    this.serviceConfigs = mapper.readValue(new File(config.serviceFile), new TypeReference<List<ServiceConfig>>(){});

                } catch (IOException e) {
                    logger.error(e);
                }
            }


            SchemaParser schemaParser = new SchemaParser();
            this.registry = schemaParser.parse(new File(config.schemaFile));

            this.name = config.name;
            this.schemaFile = config.schemaFile;
            this.serviceFile = config.serviceFile;
            this.graphqlConfig = config.graphqlConfig;

        } catch (IOException e) {
            logger.error(e);
        }


    }


    public List<ServiceConfig> getServiceConfigs() {
        return serviceConfigs;
    }

    public GraphqlConfig getGraphqlConfig() {
        return graphqlConfig;
    }

    public String getName() {
        return name;
    }

    public String getServiceFile() {
        return serviceFile;
    }

    public TypeDefinitionRegistry getRegistry() {
        return registry;
    }

}


