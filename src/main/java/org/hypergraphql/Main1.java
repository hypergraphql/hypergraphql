//package org.hypergraphql;
//
//
//import org.apache.jena.query.Dataset;
//import org.apache.jena.query.DatasetFactory;
//import org.apache.log4j.PropertyConfigurator;
//import org.hypergraphql.config.system.HGQLConfig;
//
//public class Main1 {
//
//    public static void main(String[] args) {
//
//
//        HGQLConfig config = new HGQLConfig("src/test/resources/config.json");
//        Controller.start(config);
//
//        Dataset ds = DatasetFactory.createTxnMem() ;
//        FusekiServer server = FusekiServer.create()
//                .add("/ds", ds)
//                .build() ;
//        server.start() ;
//
//
//
//        server.stop() ;
//
//    }
//}


package org.hypergraphql;

import org.hypergraphql.config.system.HGQLConfig;

public class Main1 {

    public static void main(String[] args) {


        HGQLConfig config1 = new HGQLConfig("src/test/resources/DemoServices/config1.json");
        new Controller().start(config1);
        HGQLConfig config2 = new HGQLConfig("src/test/resources/DemoServices/config2.json");
        new Controller().start(config2);
        HGQLConfig config3 = new HGQLConfig("src/test/resources/DemoServices/config3.json");
        new Controller().start(config3);
//        HGQLConfig config4 = new HGQLConfig("src/test/resources/DemoServices/config4.json");
//        new Controller().start(config4);

    }
}