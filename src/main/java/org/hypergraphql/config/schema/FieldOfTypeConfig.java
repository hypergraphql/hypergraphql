package org.hypergraphql.config.schema;

import graphql.schema.GraphQLOutputType;
import org.hypergraphql.datafetching.services.Service;

public class FieldOfTypeConfig {

    private final String id;
    private final String name;
    private final Service service;
    private final GraphQLOutputType graphqlOutputType;
    private final Boolean isList;
    private final String targetName;

    public FieldOfTypeConfig(final String name,
                             final String id,
                             final Service service,
                             final GraphQLOutputType graphqlOutputType,
                             final Boolean isList,
                             final String targetName) {

        this.name = name;
        this.id = id;
        this.service = service;
        this.graphqlOutputType = graphqlOutputType;
        this.targetName = targetName;
        this.isList = isList;
    }

    public String getId() {
        return id;
    }

    public Service getService() {
        return service;
    }

    public GraphQLOutputType getGraphqlOutputType() {
        return graphqlOutputType;
    }

    public Boolean isList() {
        return isList;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getName() {
        return name;
    }
}
