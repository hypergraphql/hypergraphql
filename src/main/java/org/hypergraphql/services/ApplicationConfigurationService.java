package org.hypergraphql.services;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.exception.HGQLConfigurationException;

@Slf4j
public class ApplicationConfigurationService {

    private S3Service s3Service;
    private final HGQLConfigService hgqlConfigService = new HGQLConfigService();

    public List<HGQLConfig> readConfigurationFromS3(final String configUri,
                                                    final String username,
                                                    final String password) {

        final URI uri;
        try {
            uri = new URI(configUri);
        } catch (URISyntaxException e) {
            throw new HGQLConfigurationException("Invalid S3 URL", e);
        }

        if (s3Service == null) {
            s3Service = new S3Service();
        }

        final InputStream inputStream = s3Service.openS3Stream(uri, username, password);
        final HGQLConfig config = hgqlConfigService.loadHGQLConfig(configUri, inputStream, username, password, false);

        return Collections.singletonList(config);
    }

    public List<HGQLConfig> readConfigurationFromUrl(final String configUri,
                                                     final String username,
                                                     final String password) {

        final GetRequest getRequest;
        if (username == null && password == null) {
            getRequest = Unirest.get(configUri);
        } else {
            getRequest = Unirest.get(configUri).basicAuth(username, password);
        }

        try {
            final InputStream inputStream = getRequest.asBinary().getBody();
            final HGQLConfig config = hgqlConfigService.loadHGQLConfig(configUri, inputStream, username, password, false);
            return Collections.singletonList(config);
        } catch (UnirestException e) {
            throw new HGQLConfigurationException("Unable to read from remote URL", e);
        }
    }

    public List<HGQLConfig> getConfigFiles(final String... configPathStrings) {

        final List<HGQLConfig> configFiles = new ArrayList<>();
        if (configPathStrings != null) {
            Arrays.stream(configPathStrings).forEach(configPathString ->
                    configFiles.addAll(getConfigurationsFromFile(configPathString)));
        }
        return configFiles;
    }

    List<HGQLConfig> getConfigurationsFromFile(final String configPathString) {

        final File configPath = new File(configPathString); // it always has this
        final List<HGQLConfig> configurations = new ArrayList<>();
        try {
            if (configPath.isDirectory()) {
                log.debug("Directory");
                final File[] jsonFiles = configPath.listFiles(pathname ->
                        FilenameUtils.isExtension(pathname.getName(), "json"));
                if (jsonFiles != null) {
                    Arrays.stream(jsonFiles).forEach(file -> {
                        final String path = file.getAbsolutePath();
                        try (InputStream in = new FileInputStream(file)) {
                            configurations.add(hgqlConfigService.loadHGQLConfig(path, in, false));
                        } catch (FileNotFoundException e) {
                            throw new HGQLConfigurationException("One or more config files not found", e);
                        } catch (IOException e) {
                            throw new HGQLConfigurationException("Unable to load configuration", e);
                        }
                    });
                }
            } else { // assume regular file
                log.debug("Regular File");
                try (InputStream in = new FileInputStream(configPath)) {
                    configurations.add(hgqlConfigService.loadHGQLConfig(configPathString, in, false));
                }
            }
        } catch (IOException e) {
            throw new HGQLConfigurationException("One or more config files not found", e);
        }
        return configurations;
    }

    private List<HGQLConfig> getConfigurationsFromClasspath(final String configPathString) {

        final String filename = getConfigFilename(configPathString);

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(filename)) {

            if (in == null) {
                // try to get from file - probably being run from an IDE with CP as filesystem
                return getConfigurationsFromFile(configPathString);
            }
            return Collections.singletonList(hgqlConfigService.loadHGQLConfig(configPathString, in, true));

        } catch (IOException e) {

            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    private String getConfigFilename(final String configPathString) {

        final String fn = configPathString.contains("!")
                ? configPathString.substring(configPathString.lastIndexOf("!") + 1)
                : configPathString;
        return configPathString.startsWith("/") ? fn : fn.substring(fn.indexOf("/") + 1);
    }

    public List<HGQLConfig> getConfigResources(final String... resourcePaths) {

        final List<HGQLConfig> configurations = new ArrayList<>();

        if (resourcePaths != null) {
            Arrays.stream(resourcePaths).forEach(
                resourcePath -> {
                    final URL sourceUrl = getClass().getClassLoader().getResource(resourcePath);

                    log.info("Resource path: {}", resourcePath);
                    if (sourceUrl != null) {
                        configurations.addAll(getConfigurationsFromClasspath(sourceUrl.getFile()));
                    }
                }
            );
        }

        return configurations;
    }
}
