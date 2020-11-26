package org.hypergraphql.services;

import java.util.Map;
import lombok.val;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HGQLQueryServiceTest {

    private final HGQLConfigService configService = new HGQLConfigService();

    @Test
    void results_with_no_accept_type() {

        val configPath = "test_config.json";
        val inputStream = getClass().getClassLoader().getResourceAsStream(configPath);
        val config = configService.loadHGQLConfig(configPath, inputStream, true);
        val service = new HGQLQueryService(config);

        val query = "{\n"
                + "Company_GET(limit:1, offset:3) {\n"
                + "  name\n"
                + "  owner {\n"
                + "    name \n"
                + "    birthPlace {\n"
                + "      label\n"
                + "    }\n"
                + "  }\n"
                + "}\n"
                + "}";

        final Map<String, Object> actual = service.results(query, null);

        assertNotNull(actual);
        assertFalse(actual.isEmpty());
        assertTrue(actual.containsKey("data"));
    }
}
