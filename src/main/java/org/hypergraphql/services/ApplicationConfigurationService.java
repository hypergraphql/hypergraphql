package org.hypergraphql.services;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.commons.io.FilenameUtils;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.exception.HGQLConfigurationException;

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

public class ApplicationConfigurationService {

    private S3Service s3Service;
    private HGQLConfigService hgqlConfigService = new HGQLConfigService();

    public List<HGQLConfig> getConfigurationFromS3(final String configUri, final String username, final String password) {

        final URI uri;
        try {
            uri = new URI(configUri);
        } catch (URISyntaxException e) {
            throw new HGQLConfigurationException("Invalid S3 URL", e);
        }

        if(s3Service == null) {
            s3Service = new S3Service();
        }

        final AmazonS3 s3 = s3Service.buildS3(uri, username, password);

        final String bucket = s3Service.extractBucket(uri);
        final String configName = s3Service.extractObjectName(uri);
        final S3Object object = s3.getObject(bucket, configName);

        final InputStream inputStream = object.getObjectContent();
        final HGQLConfig config = hgqlConfigService.loadHGQLConfig(configName, inputStream, username, password);

        return Collections.singletonList(config);
    }

    public List<HGQLConfig> getConfigFiles(final String ... configPathStrings) {

        final List<HGQLConfig> configFiles = new ArrayList<>();
        if(configPathStrings != null) {
            Arrays.stream(configPathStrings).forEach(configPathString ->
                    configFiles.addAll(getConfigurations(configPathString)));
        }
        return configFiles;
    }

    private List<HGQLConfig> getConfigurations(final String configPathString) {

        final File configPath = new File(configPathString); // it always has this
        final List<HGQLConfig> configurations = new ArrayList<>();
        try {
            if (configPath.isDirectory()) {
                final File[] jsonFiles = configPath.listFiles(pathname ->
                        FilenameUtils.isExtension(pathname.getName(), "json"));
                if (jsonFiles != null) {
                    Arrays.stream(jsonFiles).forEach(file -> {
                        final String path = file.getAbsolutePath();
                        try {
                            configurations.add(hgqlConfigService.loadHGQLConfig(path, new FileInputStream(file)));
                        } catch (FileNotFoundException e) {
                            throw new HGQLConfigurationException("One or more config files not found", e);
                        }
                    });
                }
            } else { // assume regular file
                configurations.add(hgqlConfigService.loadHGQLConfig(configPathString, new FileInputStream(configPath)));
            }
        } catch (IOException e) {
            throw new HGQLConfigurationException("One or more config files not found", e);
        }
        return configurations;
    }

    public List<HGQLConfig> getConfigResources(final String ... resourcePaths) {

        final List<HGQLConfig> configurations = new ArrayList<>();

        if(resourcePaths != null) {
            Arrays.stream(resourcePaths).forEach(
                    resourcePath -> {
                        final URL sourceUrl = getClass().getClassLoader().getResource(resourcePath);

                        if(sourceUrl != null) {
                            configurations.addAll(getConfigurations(sourceUrl.getFile()));
                        }
                    }
            );
        }

        return configurations;
    }
}
