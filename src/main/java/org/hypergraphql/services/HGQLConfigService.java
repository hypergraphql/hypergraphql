package org.hypergraphql.services;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.GetRequest;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.util.EntityUtils;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.datamodel.HGQLSchemaWiring;
import org.hypergraphql.exception.HGQLConfigurationException;

import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;

public class HGQLConfigService {

    private S3Service s3Service = new S3Service();

    private final String s3Regex = "(?i)^https?://s3.*\\.amazonaws\\.com/.*";
    private final String normalUrlRegex = "(?i)^https?://.*";

    public HGQLConfig loadHGQLConfig(final String hgqlConfigPath, final InputStream inputStream) {
        return loadHGQLConfig(hgqlConfigPath, inputStream, null, null);
    }

    public HGQLConfig loadHGQLConfig(final String hgqlConfigPath, final InputStream inputStream, final String username, final String password) {

        ObjectMapper mapper = new ObjectMapper();

        try {
            HGQLConfig config = mapper.readValue(inputStream, HGQLConfig.class);

            SchemaParser schemaParser = new SchemaParser();

            final String fullSchemaPath = extractFullSchemaPath(hgqlConfigPath, config.getSchemaFile());

            TypeDefinitionRegistry registry =
                    schemaParser.parse(selectAppropriateReader(fullSchemaPath, username, password));

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

        final GetRequest getRequest;
        if(username == null && password == null) {
            getRequest = Unirest.get(schemaPath);
        } else {
            getRequest = Unirest.get(schemaPath).basicAuth(username, password);
        }
        return new InputStreamReader(
                new ByteArrayInputStream(
                        EntityUtils.toByteArray(
                                getRequest.getBody().getEntity()
                        )
                )
        );
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

    private String extractFullSchemaPath(final String hgqlConfigPath, final String schemaPath) {

        if(isAbsolute(schemaPath)) { // FQ URL or absolute path
            return schemaPath;
        } else { // relative
            if(isAbsolute(hgqlConfigPath)) {
                final String parentPath = FilenameUtils.getPath(hgqlConfigPath);
                return parentPath + (parentPath.endsWith("/") ? "" : "/") + schemaPath;
            }
            return schemaPath.startsWith("./") ? "" : "./" + schemaPath;
        }
    }

    private boolean isAbsolute(final String path) {
        final String fileUrlRegex = "^file://.*";
        return path.matches(s3Regex) || path.matches(normalUrlRegex)
                || path.matches(fileUrlRegex) || path.startsWith("/");
    }
}
