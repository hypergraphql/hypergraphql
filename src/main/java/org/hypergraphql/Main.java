package org.hypergraphql;

import graphql.GraphQL;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class Main {

    static Logger logger = Logger.getLogger(Main.class);


    public static void main(String[] args) {

        PropertyConfigurator.configure("log4j.properties");

        Config config = new Config("properties.json");
        GraphqlWiring wiring = new GraphqlWiring(config);
        GraphQL graphQL = GraphQL.newGraphQL(wiring.schema()).build();

        System.out.println("GraphQL server started at: http://localhost:" + config.graphql().port() + config.graphql().path());
        System.out.println("GraphiQL UI available at: http://localhost:" + config.graphql().port() + config.graphql().graphiql());

        logger.info("Server started at http://localhost:" + config.graphql().port() + config.graphql().path());

        Controller.start(config, graphQL, wiring.schema());

    }
}
