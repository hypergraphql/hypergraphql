package org.hypergraphql.config.schema;

import graphql.schema.GraphQLOutputType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hypergraphql.datafetching.services.Service;

@RequiredArgsConstructor
@Getter
public class FieldOfTypeConfig {

    private final String name;
    private final String id;
    private final Service service;
    private final GraphQLOutputType graphqlOutputType;
    private final boolean isList;
    private final String targetName;

    public boolean isList() {
        return isList;
    }
}
