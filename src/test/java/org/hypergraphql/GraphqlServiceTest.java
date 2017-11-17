package org.hypergraphql;

import graphql.GraphQL;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by szymon on 01/11/2017.
 */
public class GraphqlServiceTest {

    private final int LIMIT = 100;

    private final String TEST_QUERY =
                    "{\n" +
                    "  people(limit:%s) {\n" +
                    "    name\n" +
                    "    birthDate\n" +
                    "    deathDate\n" +
                    "    birthPlace {\n" +
                    "      _id\n" +
                    "      label\n" +
                    "      country {\n" +
                    "        _id\n" +
                    "        label\n" +
                    "      }\n" +
                    "    }\n" +
                    "    deathPlace {\n" +
                    "      _id\n" +
                    "      label\n" +
                    "      country {\n" +
                    "        _id\n" +
                    "        label\n" +
                    "      }\n" +
                    "    }\n" +
                    "  }\n" +
                    "}\n";

    @Test
    public void queryPerformanceTest() {
        Config config = new Config("properties.json");
        GraphqlWiring wiring = new GraphqlWiring(config);
        GraphQL graphQL = GraphQL.newGraphQL(wiring.schema()).build();

        GraphqlService service = new GraphqlService(config, graphQL);

        String query = String.format(TEST_QUERY, LIMIT);

        long tStart = System.currentTimeMillis();

        Map<String, Object> results = service.results(query, wiring.schema());

        long tEnd = System.currentTimeMillis();
        long tDelta = tEnd - tStart;
        double elapsedSeconds = tDelta / 1000.0;

        System.out.println(elapsedSeconds);

        Map<String, Object> data = (Map<String, Object>) results.get("data");

        ArrayList people = (ArrayList) data.get("people");

        assert(people.size()==LIMIT);

    }

}
