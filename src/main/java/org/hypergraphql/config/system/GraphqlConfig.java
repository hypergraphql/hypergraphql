package org.hypergraphql.config.system;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GraphqlConfig {

    private Integer port;
    private String graphqlPath;
    private String graphiqlPath;

    @JsonCreator
    public GraphqlConfig(@JsonProperty("port") Integer port,
                         @JsonProperty("graphql") String graphqlPath,
                         @JsonProperty("graphiql") String graphiqlPath
    ) {
        this.port = port;
        this.graphqlPath = graphqlPath;
        this.graphiqlPath = graphiqlPath;
    }

    public Integer port() {
        return port;
    }
    public String graphqlPath() {
        return graphqlPath;
    }
    public String graphiqlPath() {
        return graphiqlPath;
    }
}
