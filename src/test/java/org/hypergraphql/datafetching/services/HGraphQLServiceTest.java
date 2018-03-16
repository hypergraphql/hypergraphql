package org.hypergraphql.datafetching.services;

import org.apache.jena.rdf.model.Model;
import org.hypergraphql.Controller;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.config.system.ServiceConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

//@Disabled("For the time being") // TODO - enable and fix!
class HGraphQLServiceTest {

    @Test
    @Disabled("For the time being")
    void integration_test() throws Exception {

        HGQLConfig config = new HGQLConfig("src/test/resources/config.json");
        final Controller controller = new Controller();
        controller.start(config);

        final ServiceConfig serviceConfig = prepareServiceConfig(config);

        HGraphQLService hgqlService = new HGraphQLService();
        hgqlService.setParameters(serviceConfig);

        String testQuery = "" +
                "{\n" +
                "  Person_GET(limit:10) {\n" +
                "    name\n" +
                "    _id\n" +
                "    birthDate\n" +
                "    birthPlace {\n" +
                "      label(lang:\"en\")\n" +
                "      country {\n" +
                "        _id\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";

        Method method = HGraphQLService.class.getDeclaredMethod("getModelFromRemote", String.class);
        method.setAccessible(true);
        Model model = (Model) method.invoke(hgqlService, testQuery);

        assertTrue(model.size() > 10); // TODO - this looks a bit vague

        controller.stop();
    }

    private ServiceConfig prepareServiceConfig(final HGQLConfig config) {

        return new ServiceConfig(
                null,
                null,
                "http://localhost:" + config.getGraphqlConfig().port() + config.getGraphqlConfig().graphqlPath(),
                null,
                "",
                "",
                null,
                null
        );
    }
}