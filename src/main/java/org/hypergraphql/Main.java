package org.hypergraphql;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.hypergraphql.config.system.HGQLConfig;

public class Main {

    private static Logger logger = Logger.getLogger(Main.class);
    
    public static void main(String[] args) {

        PropertyConfigurator.configure("log4j.properties");
        HGQLConfig config = new HGQLConfig("config.json");

        logger.info("Server started at http://localhost:" + config.getGraphqlConfig().port() + config.getGraphqlConfig().graphqlPath());

        Controller controller = new Controller();
        controller.start(config);

    }
}
