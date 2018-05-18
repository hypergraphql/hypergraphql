package org.hypergraphql.demo;

import org.hypergraphql.Controller;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.services.HGQLConfigService;

import java.io.InputStream;

public class ClasspathDemo {

    public static void main(String[] args) {

        final HGQLConfigService service = new HGQLConfigService();

        HGQLConfig config1 = service.loadHGQLConfig(classpathInputStream("demo_services/config1.json"));
        new Controller().start(config1); //dbpedia-hgql
        HGQLConfig config2 = service.loadHGQLConfig(classpathInputStream("demo_services/config2.json"));
        new Controller().start(config2); //agrovoc-hgql
        HGQLConfig config3 = service.loadHGQLConfig(classpathInputStream("demo_services/config3.json"));
        new Controller().start(config3); //fao-go-hgql
    }

    private static InputStream classpathInputStream(final String path) {

        return ClasspathDemo.class.getClassLoader().getResourceAsStream(path);
    }
}
