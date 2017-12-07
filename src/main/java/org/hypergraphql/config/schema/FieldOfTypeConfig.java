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

    public Boolean getIsList() {
        return isList;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getName() {
        return name;
    }

    private String id;
    private String name;
    private Service service;
    private GraphQLOutputType graphqlOutputType;
    private Boolean isList;
    private String targetName;

    public FieldOfTypeConfig(String name, String id, Service service, GraphQLOutputType graphqlOutputType, Boolean isList, String targetName) {

        this.name = name;
        this.id=id;
        this.service=service;
        this.graphqlOutputType = graphqlOutputType;
        this.targetName=targetName;
        this.isList=isList;

    }


}
