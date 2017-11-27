package org.hypergraphql;

import com.fasterxml.jackson.databind.JsonNode;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import org.hypergraphql.config.HGQLConfig;
import org.hypergraphql.datamodel.ModelContainer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
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
                    "}}\n";

    @Test
    public void fetchingDataToLocalFromQuery() {
        HGQLConfig config = new HGQLConfig("properties.json");

        List<Map<String, String>> sparqlQueries;

        String query = String.format(TEST_QUERY, LIMIT);

        Converter converter = new Converter(config);
        JsonNode jsonQuery = (JsonNode) converter.query2json(query).get("query");

        sparqlQueries = converter.graphql2sparql(converter.includeContextInQuery(jsonQuery));

        //todo
        ModelContainer client = null;

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(query)
                .context(client)
                .build();

        long tStart = System.currentTimeMillis();

        ExecutionResult qlResult = config.graphql().execute(executionInput);

        long tEnd = System.currentTimeMillis();
        long tDelta = tEnd - tStart;
        double elapsedSeconds = tDelta / 1000.0;

        System.out.println("SparqlClient fetchers: " + elapsedSeconds);

        Map<String, Object> data =  qlResult.getData();

        ArrayList people = (ArrayList) data.get("people");

        assert(people.size()==LIMIT);

    }

}
