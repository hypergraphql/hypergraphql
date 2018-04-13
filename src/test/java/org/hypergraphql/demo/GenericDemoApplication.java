package org.hypergraphql.demo;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FilenameUtils;
import org.hypergraphql.Controller;
import org.hypergraphql.config.system.HGQLConfig;

import java.io.File;
import java.util.Arrays;

/**
 * This class looks for files on the filesystem.
 * @see Demo for an example of using the classpath to access classpath resources
 */
public class GenericDemoApplication {

    public static void main(final String[] args) throws Exception {

        CommandLineParser parser = new DefaultParser();
        Options options = new Options()
                .addOption("config", "config", true, "Location of config files");
        CommandLine commandLine = parser.parse(options, args);

        if(commandLine.hasOption("config")) {

            final File configDirectory = new File(commandLine.getOptionValue("config"));

            final File[] configFiles = configDirectory.listFiles(pathname ->
                    FilenameUtils.isExtension(pathname.getName(), "json"));

            if(configFiles != null) {
                Arrays.stream(configFiles).forEach(file ->
                        new Controller().start(HGQLConfig.fromFileSystemPath(file.getPath())));
            }
        }
    }
}
