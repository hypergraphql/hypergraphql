package org.hypergraphql.services;

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
import org.hypergraphql.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

public class HGQLConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HGQLConfigService.class);
    private static final String S3_REGEX = "(?i)^https?://s3.*\\.amazonaws\\.com/.*";
    private static final String NORMAL_URL_REGEX = "(?i)^https?://.*";

    private S3Service s3Service = new S3Service();

    public HGQLConfig loadHGQLConfig(final String hgqlConfigPath, final InputStream inputStream) {
        return loadHGQLConfig(hgqlConfigPath, inputStream, null, null);
    }

    HGQLConfig loadHGQLConfig(final String hgqlConfigPath, final InputStream inputStream, final String username, final String password) {

        final ObjectMapper mapper = new ObjectMapper();

        try {

            final HGQLConfig config = mapper.readValue(inputStream, HGQLConfig.class);
            final SchemaParser schemaParser = new SchemaParser();

            final String fullSchemaPath = extractFullSchemaPath(hgqlConfigPath, config.getSchemaFile());

            LOGGER.info("Schema config path: " + fullSchemaPath);

            final Reader reader = selectAppropriateReader(fullSchemaPath, username, password);
            final TypeDefinitionRegistry registry =
                    schemaParser.parse(reader);

            final HGQLSchemaWiring wiring = new HGQLSchemaWiring(registry, config.getName(), config.getServiceConfigs());
            config.setGraphQLSchema(wiring.getSchema());
            config.setHgqlSchema(wiring.getHgqlSchema());
            return config;

        } catch (IOException | URISyntaxException e) {
            throw new HGQLConfigurationException("Error reading from configuration file", e);
        }
    }

    private Reader selectAppropriateReader(final String schemaPath, final String username, final String password)
            throws IOException, URISyntaxException {

        if(schemaPath.matches(S3_REGEX)) {

            // create S3 bucket request, etc.
            return getReaderForS3(schemaPath, username, password);

        } else if(schemaPath.matches(NORMAL_URL_REGEX)) {
            return getReaderForUrl(schemaPath, username, password);
        } else if (schemaPath.contains(".jar!")) {
            // classpath
            System.out.println("Schema on classpath");
            return getReaderForClasspath(schemaPath);
        } else {
            // file
            return new InputStreamReader(new FileInputStream(schemaPath), StandardCharsets.UTF_8);
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
                ),
                StandardCharsets.UTF_8
        );
    }

    private Reader getReaderForS3(final String schemaPath, final String username, final String password)
            throws URISyntaxException {

        final URI uri = new URI(schemaPath);
        return new InputStreamReader(s3Service.openS3Stream(uri, username, password), StandardCharsets.UTF_8);
    }

    private String extractFullSchemaPath(final String hgqlConfigPath, final String schemaPath) {

        final String configPath = FilenameUtils.getPath(hgqlConfigPath);
        return PathUtils.makeAbsolute(configPath, schemaPath);
    }

    private Reader getReaderForClasspath(final String schemaPath) {

        System.out.println("Obtaining reader: " + schemaPath);

        final String fn = schemaPath.substring(schemaPath.lastIndexOf("!") + 1);
        final String filename = fn.substring(fn.startsWith("/") ? 1 : 0);
        System.out.println("For filename: " + filename);

        return new InputStreamReader(getClass().getClassLoader().getResourceAsStream(filename));
    }
}
