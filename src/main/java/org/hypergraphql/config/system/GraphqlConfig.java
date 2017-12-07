package org.hypergraphql.config.system;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GraphqlConfig {

    private Integer port;
    private String path;
    private String graphiql;

    @JsonCreator
    public GraphqlConfig(@JsonProperty("port") Integer port,
                         @JsonProperty("path") String path,
                         @JsonProperty("graphiql") String graphiql
    ) {
        this.port = port;
        this.path = path;
        this.graphiql = graphiql;
    }

    public Integer port() {
        return port;
    }
    public String path() {
        return path;
    }
    public String graphiql() {
        return graphiql;
    }
}
