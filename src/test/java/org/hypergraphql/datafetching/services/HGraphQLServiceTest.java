package org.hypergraphql.datafetching.services;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.hypergraphql.Controller;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.config.system.ServiceConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HGraphQLServiceTest {

    @Test
    @Disabled("This is a recursive test - fix is WIP")
    void integration_test() {

        HGQLConfig config = HGQLConfig.fromFileSystemPath("src/test/resources/test_config.json");
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

        Model model = hgqlService.getModelFromRemote(testQuery);

//  Load an expected model and compare with that
//        final Model expectedModel = ModelFactory.createDefaultModel();
//        expectedModel.read()

        assertTrue(model.size() > 10); // TODO - this looks a bit vague

        controller.stop();
    }

    private ServiceConfig prepareServiceConfig(final HGQLConfig config) {

        return new ServiceConfig(
                null,
                null,
                "http://localhost:" + config.getGraphqlConfig().port() + config.getGraphqlConfig().graphQLPath(),
                null,
                "",
                "",
                null,
                null
        );
    }
}