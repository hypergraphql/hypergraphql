package org.hypergraphql.query.converters;

import com.fasterxml.jackson.databind.JsonNode;
import org.hypergraphql.Controller;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.datafetching.ExecutionForest;
import org.hypergraphql.datafetching.ExecutionForestFactory;
import org.hypergraphql.datafetching.ExecutionTreeNode;
import org.hypergraphql.datamodel.HGQLSchemaWiring;
import org.hypergraphql.query.QueryValidator;
import org.hypergraphql.query.ValidatedQuery;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HGraphQLConverterTest {

    public Boolean generateRewritingForRootReturnValidity(String inputQuery) {

        HGQLConfig config = new HGQLConfig("src/test/resources/config.json");
        Controller controller = new Controller();
        controller.start(config);
        HGraphQLConverter converter = new HGraphQLConverter(config.getHgqlSchema());

        ValidatedQuery validatedQuery = new QueryValidator(config.getSchema()).validateQuery(inputQuery);
        ExecutionForest queryExecutionForest = new ExecutionForestFactory().getExecutionForest(validatedQuery.getParsedQuery(), config.getHgqlSchema());
        ExecutionTreeNode node = queryExecutionForest.getForest().iterator().next();
        JsonNode query = node.getQuery();
        String typeName = node.getRootType();
        String gqlQuery = converter.convertToHGraphQL(query, new HashSet<>(), typeName);
        System.out.println(gqlQuery);
        ValidatedQuery testQueryValidation = new QueryValidator(config.getSchema()).validateQuery(gqlQuery);

        return testQueryValidation.getValid();

    }

    @Test
    public void rewritingValidityOfGet() {

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

        Boolean test = generateRewritingForRootReturnValidity(testQuery);
        assertTrue(test);
    }


    @Test
    public void rewritingValidityOfGetByID() {

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

        Boolean test = generateRewritingForRootReturnValidity(testQuery);
        assertTrue(test);
    }


    public Boolean generateRewritingForNonRootReturnValidity(String inputQuery, Set<String> inputSet) {

        HGQLConfig config = new HGQLConfig("src/test/resources/config.json");
        Controller controller = new Controller();
        controller.start(config);
        HGraphQLConverter converter = new HGraphQLConverter(config.getHgqlSchema());


        ValidatedQuery validatedQuery = new QueryValidator(config.getSchema()).validateQuery(inputQuery);
        ExecutionForest queryExecutionForest = new ExecutionForestFactory().getExecutionForest(validatedQuery.getParsedQuery(), config.getHgqlSchema());
        ExecutionTreeNode node = queryExecutionForest.getForest().iterator().next().getChildrenNodes().get("x_1").getForest().iterator().next();
        JsonNode query = node.getQuery();
        String typeName = node.getRootType();
        String gqlQuery = converter.convertToHGraphQL(query, inputSet, typeName);
        System.out.println(gqlQuery);
        ValidatedQuery testQueryValidation = new QueryValidator(config.getSchema()).validateQuery(gqlQuery);

        return testQueryValidation.getValid();

    }

    @Test
    public void rewritingValidityOfNonRootQuery() {

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

        Boolean test = generateRewritingForNonRootReturnValidity(query, inputSet);
        assertTrue(test);
    }
}