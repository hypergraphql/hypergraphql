package org.hypergraphql;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.sparql.algebra.Op;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.exception.HGQLConfigurationException;
import org.hypergraphql.services.ApplicationConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * This class looks for files on the filesystem.
 * See 'Demo' and 'ClasspathDemo' in test root for an example of using the classpath to access classpath resources
 */

public class Application {

    private final static Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static void main(final String[] args) throws Exception {

        final Options options = buildOptions();
        final CommandLineParser parser = new DefaultParser();
        final CommandLine commandLine = parser.parse(options, args);

        final ApplicationConfigurationService service = new ApplicationConfigurationService();

        final List<HGQLConfig> configurations;

        if(commandLine.hasOption("config") || commandLine.hasOption("s3")) {

            configurations = getConfigurationFromArgs(service, commandLine);
        } else {
            configurations = getConfigurationsFromEnvVars(service);
        }

        configurations.forEach(config -> {
            LOGGER.info("Starting controller...");
            new Controller().start(config);
        });
    }

    private static Options buildOptions() {

        return new Options()
                .addOption(
                        Option.builder("config")
                                .longOpt("config")
                                .hasArgs()
                                .numberOfArgs(Option.UNLIMITED_VALUES)
                                .desc("Location of config files (or absolute paths to config files)")
                                .build()
                )
                .addOption(
                        Option.builder("classpath")
                                .longOpt("classpath")
                                .hasArg(false)
                                .desc("Look on classpath instead of file system")
                                .build()
                ).addOption(
                        Option.builder("s3")
                                .longOpt("s3")
                                .hasArg(true)
                                .desc("Look at the provided URL for configuration")
                                .build()
                ).addOption(
                        Option.builder("u") // access key
                                .longOpt("username")
                                .hasArg(true)
                                .desc("Username (or access key ID for S3)")
                                .build()
                ).addOption(
                        Option.builder("p") // secret key
                                .longOpt("password")
                                .hasArg(true)
                                .desc("Password (or secret key for S3")
                                .build()
                ).addOption(
                        Option.builder("D")
                                .longOpt( "property=value" )
                                .hasArgs()
                                .numberOfArgs(2)
                                .valueSeparator()
                                .desc( "use value for given property" )
                                .build()
                );
    }

    private static List<HGQLConfig> getConfigurationsFromEnvVars(final ApplicationConfigurationService service) {

        // look for environment variables
        final String configPath = System.getenv("hgql_config");
        if(StringUtils.isBlank(configPath)) {
            throw new HGQLConfigurationException("No configuration parameters seem to have been provided");
        }
        final String username = System.getenv("hgql_username");
        final String password = System.getenv("hgql_password");

        LOGGER.debug("Config path: {}", configPath);
        LOGGER.debug("Username: {}", username);
        LOGGER.debug("Password: {}", password == null ? "Not provided" : "**********");

        return service.getConfigurationFromS3(configPath, username, password);
    }

    private static List<HGQLConfig> getConfigurationFromArgs(
            final ApplicationConfigurationService service,
            final CommandLine commandLine
    ) {
        if(commandLine.hasOption("s3")) {

            final String s3url = commandLine.getOptionValue("s3");
            final String accessKey = commandLine.getOptionValue('u');
            final String secretKey = commandLine.getOptionValue('p');

            // URL lookup
            return service.getConfigurationFromS3(s3url, accessKey, secretKey);
        } else if(commandLine.hasOption("config")) {

            if (commandLine.hasOption("classpath")) {
                return service.getConfigResources(commandLine.getOptionValues("config"));
            } else {
                return service.getConfigFiles(commandLine.getOptionValues("config"));
            }

        } else {

            throw new IllegalArgumentException("One of 'config' or 's3' MUST be provided");
        }
    }
}

