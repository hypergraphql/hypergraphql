package org.hypergraphql;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class Main {

    static Logger logger = Logger.getLogger(Main.class);


    public static void main(String[] args) {

        PropertyConfigurator.configure("log4j.properties");
        Config config = new Config("properties.json");

        System.out.println("GraphQL server started at: http://localhost:" + config.graphqlConfig().port() + config.graphqlConfig().path());
        System.out.println("GraphiQL UI available at: http://localhost:" + config.graphqlConfig().port() + config.graphqlConfig().graphiql());

        logger.info("Server started at http://localhost:" + config.graphqlConfig().port() + config.graphqlConfig().path());

        Controller.start(config);

    }
}
