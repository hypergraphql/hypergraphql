package org.hypergraphql.datafetching.services;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.ModelFactory;
import org.hypergraphql.config.system.ServiceConfig;
import org.hypergraphql.exception.HGQLConfigurationException;
import org.hypergraphql.services.S3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

public class S3ModelSPARQLService extends LocalModelSPARQLService {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3ModelSPARQLService.class);

    private final S3Service s3Service = new S3Service();

    @Override
    public void setParameters(ServiceConfig serviceConfig) {

        id = serviceConfig.getId();
        String fileType = serviceConfig.getFiletype();

        final InputStream inputStream = openS3Stream(serviceConfig);

        try {
            processModelLocally(inputStream, fileType);
        } catch (IOException e) {
            throw new HGQLConfigurationException("Unable to process TTL file locally", e);
        }
    }

    private InputStream openS3Stream(final ServiceConfig serviceConfig) {

        final String s3Url = serviceConfig.getUrl();
        final String accessKey = serviceConfig.getUser();
        final String secretKey = serviceConfig.getPassword();

        final URI uri;
        try {
            uri = new URI(s3Url);
        } catch (URISyntaxException e) {
            throw new HGQLConfigurationException("S3 Url is not valid", e);
        }
        return s3Service.openS3Stream(uri, accessKey, secretKey);
    }

    private void processModelLocally(final InputStream inputStream, final String fileType) throws IOException {

        final File tempFile = File.createTempFile("hgql_local_", "." + fileType.toLowerCase());

        LOGGER.info(tempFile.getAbsolutePath());

        IOUtils.copyLarge(inputStream, new FileOutputStream(tempFile));

        final String filePath = "file://" + tempFile.getAbsolutePath();
        model = ModelFactory.createDefaultModel();
        model.read(filePath, fileType);

        LOGGER.info("Loaded file: " + filePath);

        tempFile.deleteOnExit();
    }
}
