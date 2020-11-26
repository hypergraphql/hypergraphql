package org.hypergraphql;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.val;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.jena.atlas.web.MediaType;
import org.apache.jena.rdf.model.ModelFactory;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.services.HGQLConfigService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by philipcoates on 2017-10-19T20:21
 */
class ControllerTest {

    private static final String BASE_PATH = "http://localhost:";
    private static final int ONE_S_IN_MS = 1000;
    private static final int TRIPLE_COUNT = 167;
    private HGQLConfig config;

    private Controller controller;

    @BeforeEach
    void startServer() {

        val configService = new HGQLConfigService();

        val configPath = "test_config.json";
        controller = new Controller();
        config = configService.loadHGQLConfig(configPath, getClass().getClassLoader().getResourceAsStream(configPath), true);
        controller.start(config);
    }

    @AfterEach
    void stopServer() {
        if (controller != null) {
            controller.stop();
        }
    }

    @Test
    void should_get_for_graphql() throws Exception {

        Thread.sleep(ONE_S_IN_MS);
        val path = BASE_PATH + config.getGraphqlConfig().port() + config.getGraphqlConfig().graphQLPath();

        val envelope = getPath(path, "text/turtle");

        val model = ModelFactory.createDefaultModel();
        model.read(envelope.streamBody(), "", "text/turtle");
        assertTrue(model.size() > 0);
        assertEquals(TRIPLE_COUNT, model.size());
    }

    @Test
    void should_get_for_graphiql() throws Exception {

        Thread.sleep(ONE_S_IN_MS);
        val path = BASE_PATH + config.getGraphqlConfig().port() + config.getGraphqlConfig().graphiQLPath();
        val envelope = getPath(path, "application/json"); // TODO
        val contentType = envelope.getContentType();
        assertNotNull(contentType);
        val mediaType = MediaType.createFromContentType(contentType);
        assertEquals("text/html", mediaType.getContentType()); // TODO
    }

    private Envelope getPath(final String path, final String acceptHeader) throws IOException {

        final Envelope envelope;
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            val get = new HttpGet(path);
            get.addHeader("Accept", acceptHeader);
            val entity = httpClient.execute(get).getEntity();
            val body = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            envelope = new Envelope(entity.getContentType().getValue(), body);
        }
        return envelope;
    }

    private static class Envelope {

        private final String contentType;
        private final String body;

        Envelope(String contentType, String body) {
            this.contentType = contentType;
            this.body = body;
        }

        String getContentType() {
            return contentType;
        }

        InputStream streamBody() {
            return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
        }
    }
}
