package org.hypergraphql.query.converters;

import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hypergraphql.Controller;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.datafetching.ExecutionForest;
import org.hypergraphql.datafetching.ExecutionForestFactory;
import org.hypergraphql.datafetching.ExecutionTreeNode;
import org.hypergraphql.query.QueryValidator;
import org.hypergraphql.services.HGQLConfigService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class HGraphQLConverterTest {

    private Controller controller;
    private HGQLConfig config;

    @BeforeEach
    void startUp() {

        final var configPath = "test_config.json";
        final var configService = new HGQLConfigService();
        final var inputStream = getClass().getClassLoader().getResourceAsStream(configPath);
        config = configService.loadHGQLConfig(configPath, inputStream, true); // ???
        if (controller == null) {
            controller = new Controller();
            controller.start(config);
        }
    }

    @AfterEach
    void cleanUp() {

        if (controller != null) {
            controller.stop();
        }
    }

    @Test
    void rewritingValidityOfGet() {

        final var testQuery = "{\n"
                + "Company_GET(limit:1, offset:3) {\n"
                + "  name\n"
                + "  owner {\n"
                + "    name \n"
                + "    birthPlace {\n"
                + "      label(lang:en)\n"
                + "    }\n"
                + "  }\n"
                + "}\n"
                + "}";

        final var test = generateRewritingForRootReturnValidity(testQuery);
        assertTrue(test);
    }

    @Test
    void rewritingValidityOfGetByID() {

        final var testQuery = "{\n"
                + "Company_GET_BY_ID(uris:[\"http://test1\", \"http://test2\", \"http://test3\"]) {\n"
                + "  name\n"
                + "  owner {\n"
                + "    name \n"
                + "    birthPlace {\n"
                + "      label(lang:\"en\")\n"
                + "    }\n"
                + "  }\n"
                + "}\n"
                + "}";

        final var test = generateRewritingForRootReturnValidity(testQuery);
        assertTrue(test);
    }

    @Test
    void rewritingValidityOfNonRootQuery() {

        final var query = "{\n"
                + "  Person_GET(limit:10) {\n"
                + "    name\n"
                + "    _id\n"
                + "    birthDate\n"
                + "    birthPlace {\n"
                + "      label(lang:\"en\")\n"
                + "    }\n"
                + "  }\n"
                + "}";

        final Set<String> inputSet = Set.of(
            "http://test1",
            "http://test2",
            "http://test3"
        );

        final var test = generateRewritingForNonRootReturnValidity(query, inputSet);
        assertTrue(test);
    }

    @Test
    void simple_fragment() {

        final var query = "fragment PersonAttrs on Person {\n"
                + "  prefLabel(lang: \"en\")\n"
                + "  altLabel\n"
                + "}\n"
                + "\n"
                + "{\n"
                + "  Person_GET(limit: 10) { \n"
                + "    _id\n"
                + "    ...PersonAttrs\n"
                + "  }\n"
                + "}";

        assertTrue(generateRewritingForRootReturnValidity(query));
    }

    @Test
    void nested_fragment() {

        final var query = "fragment PersonAttrs on Person {\n"
                + "  prefLabel(lang: \"en\")\n"
                + "  altLabel\n"
                + "}\n"
                + "\n"
                + "fragment PersonRecursive on Person {\n"
                + "\t...PersonAttrs\n"
                + "  broader {\n"
                + "    ...PersonAttrs\n"
                + "     broader{\n"
                + "       ...PersonAttrs\n"
                + "       broader {\n"
                + "         ...PersonAttrs\n"
                + "         broader {\n"
                + "           ...PersonAttrs\n"
                + "         }\n"
                + "       }\n"
                + "     }\n"
                + "  }\n"
                + "  narrower {\n"
                + "    ...PersonAttrs\n"
                + "  }\n"
                + "}\n"
                + "\n"
                + "{\n"
                + "  Person_GET(limit: 100) { \n"
                + "    _id\n"
                + "  \t...PersonRecursive\n"
                + "  }\n"
                + "}";

        assertTrue(generateRewritingForRootReturnValidity(query));
    }

    private boolean generateRewritingForRootReturnValidity(String inputQuery) {

        final var queryExecutionForest = buildForestFromQuery(inputQuery);
        final var node = queryExecutionForest.getForest().iterator().next();
        return generateRewritingForValidity(inputQuery, node, Set.of());
    }

    private boolean generateRewritingForNonRootReturnValidity(String inputQuery, Set<String> inputSet) {

        final var queryExecutionForest = buildForestFromQuery(inputQuery);
        final var node = queryExecutionForest.getForest()
                .iterator()
                .next()
                .getChildrenNodes()
                .get("x_1")
                .getForest()
                .iterator()
                .next();
        return generateRewritingForValidity(inputQuery, node, inputSet);
    }

    private boolean generateRewritingForValidity(final String inputQuery, final ExecutionTreeNode node, final Set<String> inputSet) {

        final var query = node.getQuery();
        final var typeName = node.getRootType();
        final var gqlQuery = HGraphQLConverter.convertToHGraphQL(config.getHgqlSchema(), query, inputSet, typeName);
        log.debug(gqlQuery);
        final var testQueryValidation = new QueryValidator(config.getSchema()).validateQuery(gqlQuery);

        return testQueryValidation.getValid();
    }

    private ExecutionForest buildForestFromQuery(final String inputQuery) {

        final var validatedQuery = new QueryValidator(config.getSchema()).validateQuery(inputQuery);
        return new ExecutionForestFactory().getExecutionForest(validatedQuery.getParsedQuery(), config.getHgqlSchema());
    }
}
