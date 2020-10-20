package org.hypergraphql.config.system;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class GraphqlConfig {

    private static final int PORT_RANGE_START = 1024;
    private static final int PORT_RANGE_END = 65536;

    private final Integer port;
    private final String graphqlPath;
    private final String graphiqlPath;

    @JsonCreator
    public GraphqlConfig(@JsonProperty("port") final Integer port,
                         @JsonProperty("graphql") final String graphqlPath,
                         @JsonProperty("graphiql") final String graphiqlPath
    ) {
        this.port = Objects.requireNonNullElseGet(port, this::generateRandomPort);
        this.graphqlPath = graphqlPath;
        this.graphiqlPath = graphiqlPath;
    }

    public Integer port() {
        return port;
    }

    @Deprecated
    public String graphqlPath() {
        return graphQLPath();
    }

    public String graphQLPath() {
        return graphqlPath;
    }

    @Deprecated
    public String graphiqlPath() {
        return graphiQLPath();
    }

    public String graphiQLPath() {
        return graphiqlPath;
    }

    @JsonIgnore
    private int generateRandomPort() {
        return ThreadLocalRandom.current().nextInt(PORT_RANGE_START, PORT_RANGE_END);
    }
}
