package org.hypergraphql.demo;

import org.hypergraphql.Application;

public class CLIDemo {

    public static void main(String[] args) throws Exception {

        // --classpath --config 20180515/space/config1.json 20180515/space/config2.json 20180515/space/config3.json
//        String[] demoArgs = {
//             "--classpath",
//             "--config",
//             "20180515/config1.json",
//             "20180515/config2.json",
//             "20180515/config3.json"
//        };
//
//        Application.main(demoArgs);
        Application.main(args);
    }
}
