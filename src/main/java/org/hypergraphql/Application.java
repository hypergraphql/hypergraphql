package org.hypergraphql;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FilenameUtils;
import org.hypergraphql.config.system.HGQLConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class looks for files on the filesystem.
 * See 'Demo' and 'ClasspathDemo' in test root for an example of using the classpath to access classpath resources
 */
public class Application {

    private final static Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static void main(final String[] args) throws Exception {

        final String[] trimmedArgs = trimValues(args);

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
                );
        CommandLine commandLine = parser.parse(options, trimmedArgs);

        if(!commandLine.hasOption("config")) {
            System.err.println("============ HyperGraphQL ============\n");
            System.err.println("Error: '--config' option, with at least one argument, " +
                    "e.g. '--config /home/hgql/configs/myconfig.json' is required");
            System.err.println("\n============ HyperGraphQL ============");
            return;
        }

        final ApplicationConfigurationService service = new ApplicationConfigurationService();

        final List<File> configurations;
        if(commandLine.hasOption("classpath")) {

            configurations = service.getConfigResources(commandLine.getOptionValues("config"));
        } else {
            configurations = service.getConfigFiles(commandLine.getOptionValues("config"));
        }

        configurations.forEach(file -> {
            LOGGER.info("Starting with " + file.getPath());
            new Controller().start(HGQLConfig.fromFileSystemPath(file.getPath()));
        });
    }

    private static String[] trimValues(final String[] input) {

        return Arrays.stream(input)
                .map(String::trim)
                .collect(Collectors.toList())
                .toArray(new String[input.length]);
    }
}

class ApplicationConfigurationService {

    List<File> getConfigFiles(final String ... configPathStrings) {

        final List<File> configFiles = new ArrayList<>();
        for(final String configPathString : configPathStrings) {
            configFiles.addAll(getConfigurations(configPathString));
        }
        return configFiles;
    }

    private List<File> getConfigurations(final String configPathString) {

        final List<File> configFiles = new ArrayList<>();
        final File configPath = new File(configPathString); // it always has this
        if (configPath.isDirectory()) {
            final File[] jsonFiles = configPath.listFiles(pathname ->
                    FilenameUtils.isExtension(pathname.getName(), "json"));
            if(jsonFiles != null) {
                configFiles.addAll(Arrays.asList(jsonFiles));
            }
        } else { // assume regular file
            configFiles.add(configPath);
        }
        return configFiles;
    }

    List<File> getConfigResources(final String ... resourcePaths) {

        final List<File> configFiles = new ArrayList<>();

        for(final String resourcePath : resourcePaths) {

            final URL sourceUrl = Application.class.getClassLoader().getResource(resourcePath);

            if(sourceUrl != null) {
                configFiles.addAll(getConfigurations(sourceUrl.getFile()));
            }
        }

        return configFiles;
    }
}