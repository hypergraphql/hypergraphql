package org.hypergraphql.config.schema;

import graphql.schema.GraphQLOutputType;
import org.hypergraphql.datafetching.services.Service;

public class FieldOfTypeConfig {

    public String getId() {
        return id;
    }

    public Service getService() {
        return service;
    }

    public GraphQLOutputType getGraphqlOutputType() {
        return graphqlOutputType;
    }

    public String getTypeName() {
        return typeName;
    }

    private String id;
    private Service service;
    private GraphQLOutputType graphqlOutputType;
    private String typeName;

    public FieldOfTypeConfig(String id, Service service, GraphQLOutputType graphqlOutputType, String typeName) {

        this.id=id;
        this.service=service;
        this.graphqlOutputType = graphqlOutputType;
        this.typeName=typeName;

    }


}
