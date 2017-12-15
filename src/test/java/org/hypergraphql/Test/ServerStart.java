package org.hypergraphql.Test;

import org.hypergraphql.Controller;
import org.hypergraphql.config.system.HGQLConfig;

public class ServerStart implements Runnable{


    private final String propertiesPath;

    public  ServerStart(String propertiesPath){
        this.propertiesPath = propertiesPath;


    }

    @Override
    public void run() {

        HGQLConfig config = new HGQLConfig(propertiesPath);

        Controller controller = new Controller();
        controller.start(config);

    }
}
