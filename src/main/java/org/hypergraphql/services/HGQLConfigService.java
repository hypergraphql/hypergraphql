package org.hypergraphql.services;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.datamodel.HGQLSchemaWiring;
import org.hypergraphql.exception.HGQLConfigurationException;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class HGQLConfigService {

    private S3Service s3Service = new S3Service();

    public HGQLConfig loadHGQLConfig(final InputStream inputStream) {
        return loadHGQLConfig(inputStream, null, null);
    }

    public HGQLConfig loadHGQLConfig(final InputStream inputStream, final String username, final String password) {

        ObjectMapper mapper = new ObjectMapper();

        try {
            HGQLConfig config = mapper.readValue(inputStream, HGQLConfig.class);

            SchemaParser schemaParser = new SchemaParser();
            TypeDefinitionRegistry registry =
                    schemaParser.parse(selectAppropriateReader(config.getSchemaFile(), username, password));

            HGQLSchemaWiring wiring = new HGQLSchemaWiring(registry, config.getName(), config.getServiceConfigs());
            config.setGraphQLSchema(wiring.getSchema());
            config.setHgqlSchema(wiring.getHgqlSchema());
            return config;

        } catch (IOException | URISyntaxException e) {
            throw new HGQLConfigurationException("Error reading from configuration file", e);
        }
    }

    private Reader selectAppropriateReader(final String schemaPath, final String username, final String password)
            throws IOException, URISyntaxException {

        final String s3Regex = "(?i)^https?://s3.*\\.amazonaws\\.com/.*";
        final String normalUrlRegex = "(?i)^https?://.*";
        if(schemaPath.matches(s3Regex)) {

            // create S3 bucket request, etc.
            return getReaderForS3(schemaPath, username, password);

        } else if(schemaPath.matches(normalUrlRegex)) {
            return getReaderForUrl(schemaPath, username, password);
        } else {
            // file
            return new FileReader(schemaPath);
        }
    }

    private Reader getReaderForUrl(final String schemaPath, final String username, final String password)
            throws IOException {

        return new InputStreamReader(new URL(schemaPath).openStream());
    }

    private Reader getReaderForS3(final String schemaPath, final String username, final String password)
            throws URISyntaxException {

        final URI uri = new URI(schemaPath);
        final AmazonS3 s3 = s3Service.buildS3(uri, username, password);
        final String bucket = s3Service.extractBucket(uri);
        final String objectName = s3Service.extractObjectName(uri);
        final S3Object s3Object = s3.getObject(bucket, objectName);
        return new InputStreamReader(s3Object.getObjectContent());
    }
}
