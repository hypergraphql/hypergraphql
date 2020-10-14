package org.hypergraphql.config.system;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.concurrent.ThreadLocalRandom;

public class GraphqlConfig {

    private static final int MIN_PORT = 1024;
    private static final int MAX_PORT = 65536;

    private final Integer port;
    private final String graphqlPath;
    private final String graphiqlPath;

    @JsonCreator
    public GraphqlConfig(@JsonProperty("port") final Integer port,
                         @JsonProperty("graphql") final String graphqlPath,
                         @JsonProperty("graphiql") final String graphiqlPath
    ) {
        if (port == null) {
            this.port = generateRandomPort();
        } else {
            this.port = port;
        }
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
        return ThreadLocalRandom.current().nextInt(MIN_PORT, MAX_PORT); // TODO - review this
    }
}
