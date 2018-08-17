package org.hypergraphql.query.converters;

import com.fasterxml.jackson.databind.JsonNode;
import org.hypergraphql.Controller;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.datafetching.ExecutionForest;
import org.hypergraphql.datafetching.ExecutionForestFactory;
import org.hypergraphql.datafetching.ExecutionTreeNode;
import org.hypergraphql.query.QueryValidator;
import org.hypergraphql.query.ValidatedQuery;
import org.hypergraphql.services.HGQLConfigService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HGraphQLConverterTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(HGraphQLConverterTest.class);

    private Controller controller;
    private HGQLConfig config;

    @BeforeEach
    void startUp() {

        final String configPath = "test_config.json";
        final HGQLConfigService configService = new HGQLConfigService();
        final InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configPath);
        config = configService.loadHGQLConfig(configPath, inputStream, true); // ???
        if(controller == null) {
            controller = new Controller();
            controller.start(config);
        }
    }

    @AfterEach
    void cleanUp() {

        if(controller != null) {
            controller.stop();
        }
    }

    @Test
    void rewritingValidityOfGet() {

        String testQuery = "" +
                "{\n" +
                "Company_GET(limit:1, offset:3) {\n" +
                "  name\n" +
                "  owner {\n" +
                "    name \n" +
                "    birthPlace {\n" +
                "      label(lang:en)\n" +
                "    }\n" +
                "  }\n" +
                "}\n" +
                "}";

        boolean test = generateRewritingForRootReturnValidity(testQuery);
        assertTrue(test);
    }


    @Test
    void rewritingValidityOfGetByID() {

        String testQuery = "" +
                "{\n" +
                "Company_GET_BY_ID(uris:[\"http://test1\", \"http://test2\", \"http://test3\"]) {\n" +
                "  name\n" +
                "  owner {\n" +
                "    name \n" +
                "    birthPlace {\n" +
                "      label(lang:\"en\")\n" +
                "    }\n" +
                "  }\n" +
                "}\n" +
                "}";

        boolean test = generateRewritingForRootReturnValidity(testQuery);
        assertTrue(test);
    }

    @Test
    void rewritingValidityOfNonRootQuery() {

        String query = "" +
                "{\n" +
                "  Person_GET(limit:10) {\n" +
                "    name\n" +
                "    _id\n" +
                "    birthDate\n" +
                "    birthPlace {\n" +
                "      label(lang:\"en\")\n" +
                "    }\n" +
                "  }\n" +
                "}";

        Set<String> inputSet = new HashSet<String>() {{
            add("http://test1");
            add("http://test2");
            add("http://test3");
        }};

        boolean test = generateRewritingForNonRootReturnValidity(query, inputSet);
        assertTrue(test);
    }

    // TODO - These methods seem a little complicated
    private boolean generateRewritingForRootReturnValidity(String inputQuery) {

        HGraphQLConverter converter = new HGraphQLConverter(config.getHgqlSchema());

        ValidatedQuery validatedQuery = new QueryValidator(config.getSchema()).validateQuery(inputQuery);
        ExecutionForest queryExecutionForest = new ExecutionForestFactory().getExecutionForest(validatedQuery.getParsedQuery(), config.getHgqlSchema());
        ExecutionTreeNode node = queryExecutionForest.getForest().iterator().next();
        JsonNode query = node.getQuery();
        String typeName = node.getRootType();
        String gqlQuery = converter.convertToHGraphQL(query, new HashSet<>(), typeName);
        LOGGER.debug(gqlQuery);
        ValidatedQuery testQueryValidation = new QueryValidator(config.getSchema()).validateQuery(gqlQuery);

        return testQueryValidation.getValid();
    }

    private boolean generateRewritingForNonRootReturnValidity(String inputQuery, Set<String> inputSet) {

        HGraphQLConverter converter = new HGraphQLConverter(config.getHgqlSchema());

        ValidatedQuery validatedQuery = new QueryValidator(config.getSchema()).validateQuery(inputQuery);
        ExecutionForest queryExecutionForest = new ExecutionForestFactory().getExecutionForest(validatedQuery.getParsedQuery(), config.getHgqlSchema());
        ExecutionTreeNode node = queryExecutionForest.getForest().iterator().next().getChildrenNodes().get("x_1").getForest().iterator().next();
        JsonNode query = node.getQuery();
        String typeName = node.getRootType();
        String gqlQuery = converter.convertToHGraphQL(query, inputSet, typeName);
        LOGGER.debug(gqlQuery);
        ValidatedQuery testQueryValidation = new QueryValidator(config.getSchema()).validateQuery(gqlQuery);

        return testQueryValidation.getValid();
    }
}