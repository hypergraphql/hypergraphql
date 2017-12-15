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
//        HGQLConfig config = new HGQLConfig("src/test/resources/properties.json");
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
