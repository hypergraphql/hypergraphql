package org.hypergraphql.demo;

import org.hypergraphql.Controller;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.services.HGQLConfigService;

import java.io.InputStream;

public class ClasspathDemo {

    public static void main(String[] args) {

        final HGQLConfigService service = new HGQLConfigService();

        final String cfg1 = "demo_services/config1.json";
        HGQLConfig config1 = service.loadHGQLConfig(cfg1, classpathInputStream(cfg1), true);
        new Controller().start(config1); //dbpedia-hgql
        final String cfg2 = "demo_services/config2.json";
        HGQLConfig config2 = service.loadHGQLConfig(cfg2, classpathInputStream(cfg2), true);
        new Controller().start(config2); //agrovoc-hgql
        final String cfg3 = "demo_services/config3.json";
        HGQLConfig config3 = service.loadHGQLConfig(cfg3, classpathInputStream(cfg3), true);
        new Controller().start(config3); //fao-go-hgql
    }

    private static InputStream classpathInputStream(final String path) {

        return ClasspathDemo.class.getClassLoader().getResourceAsStream(path);
    }
}
