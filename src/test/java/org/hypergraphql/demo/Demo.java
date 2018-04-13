package org.hypergraphql.demo;

import org.hypergraphql.Controller;
import org.hypergraphql.config.system.HGQLConfig;

public class Demo {

    public static void main(final String[] args) {

        HGQLConfig config1 = HGQLConfig.fromClasspathConfig("demo_services/config1.json");
        new Controller().start(config1); //dbpedia-hgql
        HGQLConfig config2 = HGQLConfig.fromClasspathConfig("demo_services/config2.json");
        new Controller().start(config2); //agrovoc-hgql
        HGQLConfig config3 = HGQLConfig.fromClasspathConfig("demo_services/config3.json");
        new Controller().start(config3); //fao-go-hgql
    }
}
