package org.hypergraphql;

import org.apache.log4j.PropertyConfigurator;
import org.hypergraphql.config.system.HGQLConfig;

public class Main1 {

    public static void main(String[] args) {



        HGQLConfig configtest = new HGQLConfig("src/test/resources/properties.json");
        Controller.start(configtest);

    }
}
