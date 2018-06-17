package org.hypergraphql;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.exception.HGQLConfigurationException;
import org.hypergraphql.services.ApplicationConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.hypergraphql.util.PathUtils.isNormalURL;
import static org.hypergraphql.util.PathUtils.isS3;

/**
 * This class looks for files on the filesystem.
 * See 'Demo' and 'ClasspathDemo' in test root for an example of using the classpath to access classpath resources
 */

public class Application {

    private final static Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static void main(final String[] args) throws Exception {

        final ApplicationConfigurationService service = new ApplicationConfigurationService();

        start(getConfigurationsFromProperties(System.getenv(), service));
    }

    protected static void start(final List<HGQLConfig> configurations) throws IOException {
        start(configurations, true);
    }

    protected static void start(final List<HGQLConfig> configurations, final boolean showBanner) throws IOException {

        if(showBanner) {
            showBanner();
        }

        configurations.forEach(config -> {
            LOGGER.info("Starting controller...");
            new Controller().start(config);
        });
    }

    static List<HGQLConfig> getConfigurationsFromProperties(
            final Map<String, String> properties,
            final ApplicationConfigurationService service
    ) {

        // look for environment variables
        final String configPath = properties.get("hgql_config");
        if(StringUtils.isBlank(configPath)) {
            throw new HGQLConfigurationException("No configuration parameters seem to have been provided");
        }
        final String username = properties.get("hgql_username");
        final String password = properties.get("hgql_password");

        LOGGER.debug("Config path: {}", configPath);
        LOGGER.debug("Username: {}", username);
        LOGGER.debug("Password: {}", password == null ? "Not provided" : "**********");

        // TODO - check for URL, S3, file://, 'file' and 'classpath:'
        if(isS3(configPath)) {
            return service.readConfigurationFromS3(configPath, username, password);
        } else if(isNormalURL(configPath)) {
            return service.readConfigurationFromUrl(configPath, username, password);
        } else {
            // assume it's a normal file
            return service.getConfigFiles(configPath);
        }
    }

    private static void showBanner() throws IOException {

        final String bannerFile = "banner.txt";
        final InputStream bannerInputStream = Application.class.getClassLoader().getResourceAsStream(bannerFile);
        if(bannerInputStream == null) {
            System.out.println(Application.class.getClassLoader().getResource(bannerFile));
            System.err.println("Banner is null");
        }
        IOUtils.copy(bannerInputStream, System.out);
        final String version = System.getProperty("hgql_version");
        if(version == null) {
            System.out.println("----------------------------------------------------------------------\n");
        } else {
            System.out.printf("------------------------------- v%1$s -------------------------------%n%n", version);
        }
    }
}

