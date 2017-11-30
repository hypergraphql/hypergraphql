package org.hypergraphql;

import com.fasterxml.jackson.databind.JsonNode;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.introspection.Introspection;
import graphql.introspection.IntrospectionQuery;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.datamodel.ModelContainer;
import org.hypergraphql.query.Converter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by szymon on 01/11/2017.
 */
public class HGQLServiceTest {

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
        HGQLConfig config = HGQLConfig.getInstance();

        List<Map<String, String>> sparqlQueries;

        String query = String.format(TEST_QUERY, LIMIT);

        Converter converter = new Converter();
        JsonNode jsonQuery = (JsonNode) converter.query2json(query).get("query");

        sparqlQueries = converter.graphql2sparql(converter.includeContextInQuery(jsonQuery));

        //todo
        ModelContainer client = null;

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(query)
       //         .context(client)
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

    @Test
    public void introspectionQuery() {
        HGQLConfig config = HGQLConfig.getInstance();

        System.out.println(config.mapping());

        String query = IntrospectionQuery.INTROSPECTION_QUERY;

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(query)
                .build();

        ExecutionResult qlResult = config.graphql().execute(executionInput);

        assert(qlResult.getErrors().isEmpty());

    }

}
