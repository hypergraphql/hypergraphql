package org.hypergraphql;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;
import org.hypergraphql.config.system.HGQLConfig;

public class Application {

    private final static Logger LOGGER = Logger.getLogger(Application.class);

    private final static String DEFAULT_CONFIG_FILE = "config.json";

    private static Controller controller;

    public static void main(String[] args) throws Exception {

        CommandLineParser parser = new DefaultParser();
        Options options = new Options()
                .addOption("c", "config", true, "Location of config file");
        CommandLine commandLine = parser.parse(options, args);

        final String configPath = commandLine.getOptionValue("config");
        HGQLConfig config = HGQLConfig.fromFileSystemPath(configPath);

        controller = new Controller();
        controller.start(config);

        LOGGER.info("Server started at http://localhost:" + config.getGraphqlConfig().port() + config.getGraphqlConfig().graphQLPath());
    }

    public static void stop() {

        if(controller != null) {
            controller.stop();
        }
    }
}
