package org.hypergraphql.services;

import org.hypergraphql.config.system.HGQLConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HGQLQueryServiceTest {

    @Test
    void results_with_no_accept_type() {

        HGQLConfig config = new HGQLConfig("config.json");
        HGQLQueryService service = new HGQLQueryService(config);

        final String query = "" +
                "{\n" +
                "Company_GET(limit:1, offset:3) {\n" +
                "  name\n" +
                "  owner {\n" +
                "    name \n" +
                "    birthPlace {\n" +
                "      label\n" +
                "    }\n" +
                "  }\n" +
                "}\n" +
                "}";

        final Map<String, Object> actual = service.results(query, null);

        assertNotNull(actual);
        assertFalse(actual.isEmpty());
        assertTrue(actual.containsKey("data"));
    }

    private void queryValidation() {


    }

}