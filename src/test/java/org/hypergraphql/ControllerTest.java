package org.hypergraphql;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.GetRequest;
import org.hypergraphql.config.system.HGQLConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Created by philipcoates on 2017-10-19T20:21
 */
class ControllerTest {

    private HGQLConfig config;

    private final String basePath = "http://localhost:";

    @BeforeEach
    void startServer() throws Exception {

        config = HGQLConfig.from(getClass().getClassLoader().getResourceAsStream("test_config.json"));
        Controller controller = new Controller();
        controller.start(config);
    }

    @Test
    void should_get_for_graphql() throws Exception {

        final String path = basePath + config.getGraphqlConfig().port() + config.getGraphqlConfig().graphQLPath();

        final GetRequest getRequest = Unirest.get(path).header("Accept", "application/ld+json");

        final HttpResponse<String> response =
                Unirest.get(path)
                        .header("Accept", "application/ld+json")
                        .asString();

        assertNotNull(response.getBody());
    }

    @Test
    void should_get_for_graphiql() throws Exception {

        final String path = basePath + config.getGraphqlConfig().port() + config.getGraphqlConfig().graphiQLPath();

        final HttpResponse<JsonNode> response =
            Unirest.get(path)
                .header("Accept", "application/json")
                .asJson();

        assertNotNull(response.getBody());
    }
}