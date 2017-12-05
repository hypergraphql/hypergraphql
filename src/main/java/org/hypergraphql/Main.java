package org.hypergraphql;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.hypergraphql.config.system.HGQLConfig;

public class Main {

    static Logger logger = Logger.getLogger(Main.class);
    
    public static void main(String[] args) {

        PropertyConfigurator.configure("log4j.properties");
        HGQLConfig config = new HGQLConfig("properties.json");
//        config.init();

        System.out.println("GraphQL server started at: http://localhost:" + config.getGraphqlConfig().port() + config.getGraphqlConfig().path());
        System.out.println("GraphiQL UI available at: http://localhost:" + config.getGraphqlConfig().port() + config.getGraphqlConfig().graphiql());

        logger.info("Server started at http://localhost:" + config.getGraphqlConfig().port() + config.getGraphqlConfig().path());

        Controller.start();

    }
}
