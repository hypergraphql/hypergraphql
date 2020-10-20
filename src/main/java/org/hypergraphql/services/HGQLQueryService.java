package org.hypergraphql.services;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.schema.GraphQLSchema;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.datafetching.ExecutionForest;
import org.hypergraphql.datafetching.ExecutionForestFactory;
import org.hypergraphql.datamodel.HGQLSchema;
import org.hypergraphql.datamodel.ModelContainer;
import org.hypergraphql.query.QueryValidator;
import org.hypergraphql.query.ValidatedQuery;

/**
 * Created by szymon on 01/11/2017.
 */
public class HGQLQueryService {

    private final GraphQL graphql;
    private final GraphQLSchema schema;
    private final HGQLSchema hgqlSchema;

    public HGQLQueryService(final HGQLConfig config) {
        this.hgqlSchema = config.getHgqlSchema();
        this.schema = config.getSchema();

        this.graphql = GraphQL.newGraphQL(config.getSchema()).build();
    }

    public Map<String, Object> results(final String query, final String acceptType) {

        final Map<String, Object> result = new HashMap<>();
        final Map<String, Object> data = new HashMap<>();
        final Map<Object, Object> extensions = new HashMap<>();
        final List<GraphQLError> errors = new ArrayList<>();

        result.put("errors", errors);
        result.put("extensions", extensions); // TODO - is this ever populated

        final ExecutionInput executionInput;
        ExecutionResult qlResult = null;

        final ValidatedQuery validatedQuery = new QueryValidator(schema).validateQuery(query);

        if (!validatedQuery.getValid()) {
            errors.addAll(validatedQuery.getErrors());
            return result;
        }

        if (query.contains("IntrospectionQuery") || query.contains("__")) {

            qlResult = graphql.execute(query);
            data.putAll(qlResult.getData());

        } else {

            final ExecutionForest queryExecutionForest =
                    new ExecutionForestFactory().getExecutionForest(validatedQuery.getParsedQuery(), hgqlSchema);

            final ModelContainer client = new ModelContainer(queryExecutionForest.generateModel());

            if (acceptType == null) {
                executionInput = ExecutionInput.newExecutionInput()
                        .query(query)
                        .context(client)
                        .build();

                qlResult = graphql.execute(executionInput);

                data.putAll(qlResult.getData());
                data.put("@context", queryExecutionForest.getFullLdContext());
            } else {
                result.put("data", client.getDataOutput(acceptType));
            }
        }

        if (qlResult != null) {
            result.put("data", data);
            errors.addAll(qlResult.getErrors());
        }
        return result;
    }
}
