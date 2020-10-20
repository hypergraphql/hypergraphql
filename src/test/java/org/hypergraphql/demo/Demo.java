package org.hypergraphql.demo;

import org.hypergraphql.Application;

public abstract class Demo {

    public static void main(final String[] args) throws Exception {

        final String[] demoArguments = {
            "--classpath",
            "--config",
            "demo_services/config1.json",
            "demo_services/config2.json",
            "demo_services/config3.json",
            "demo_services/config4.json"
        };

        Application.main(demoArguments);
    }
}
