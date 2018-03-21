package org.hypergraphql;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;
import org.hypergraphql.config.system.HGQLConfig;

public class Application {

    private final static Logger logger = Logger.getLogger(Application.class);

    private final static String DEFAULT_CONFIG_FILE = "config.json";

    public static void main(String[] args) throws Exception {

        CommandLineParser parser = new DefaultParser();
        Options options = new Options()
                .addOption(
                        Option.builder()
                            .argName("config")
                            .longOpt("config")
                            .required(false)
                            .build()
                );
        CommandLine commandLine = parser.parse(options, args);

        HGQLConfig config = HGQLConfig.fromFileSystemPath(commandLine.getOptionValue("config", DEFAULT_CONFIG_FILE));

        logger.info("Server started at http://localhost:" + config.getGraphqlConfig().port() + config.getGraphqlConfig().graphQLPath());

        final Controller controller = new Controller();
        controller.start(config);
    }
}
