package org.hypergraphql.demo;

import java.io.InputStream;
import lombok.val;
import org.hypergraphql.Controller;
import org.hypergraphql.services.HGQLConfigService;

public abstract class ClasspathDemo {

    public static void main(String[] args) {

        val service = new HGQLConfigService();

        val cfg1 = "demo_services/config1.json";
        val config1 = service.loadHGQLConfig(cfg1, classpathInputStream(cfg1), true);
        new Controller().start(config1); //dbpedia-hgql
        val cfg2 = "demo_services/config2.json";
        val config2 = service.loadHGQLConfig(cfg2, classpathInputStream(cfg2), true);
        new Controller().start(config2); //agrovoc-hgql
        final String cfg3 = "demo_services/config3.json";
        val config3 = service.loadHGQLConfig(cfg3, classpathInputStream(cfg3), true);
        new Controller().start(config3); //fao-go-hgql
    }

    private static InputStream classpathInputStream(final String path) {

        return ClasspathDemo.class.getClassLoader().getResourceAsStream(path);
    }
}
