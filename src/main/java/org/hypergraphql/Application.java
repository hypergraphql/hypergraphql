package org.hypergraphql;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.services.ApplicationConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

/**
 * This class looks for files on the filesystem.
 * See 'Demo' and 'ClasspathDemo' in test root for an example of using the classpath to access classpath resources
 */
public class Application {

    private final static Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static void main(final String[] args) throws Exception {

        final CommandLine commandLine = readCommandLine(args);

        final ApplicationConfigurationService service = new ApplicationConfigurationService();

        final List<HGQLConfig> configurations;
        if(commandLine.hasOption("s3")) {

            final String s3url = commandLine.getOptionValue("s3");
            final String accessKey = commandLine.getOptionValue('u');
            final String secretKey = commandLine.getOptionValue('p');

            // URL lookup
            configurations = service.getConfigurationFromS3(s3url, accessKey, secretKey);
        } else if(commandLine.hasOption("config")) {
            if (commandLine.hasOption("classpath")) {
                configurations = service.getConfigResources(commandLine.getOptionValues("config")); // TODO - as streams!
            } else {
                configurations = service.getConfigFiles(commandLine.getOptionValues("config"));
            }
        } else {
            throw new IllegalArgumentException("One of 'config' or 's3' MUST be provided");
        }


        configurations.forEach(config -> {
            LOGGER.info("Starting controller...");
            new Controller().start(config);
        });
    }

    private static CommandLine readCommandLine(final String ... args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options()
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
                );
        return parser.parse(options, args);
    }
}

