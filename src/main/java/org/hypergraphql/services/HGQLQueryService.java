package org.hypergraphql.services;

import graphql.*;
import graphql.schema.GraphQLSchema;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.datamodel.HGQLSchema;
import org.hypergraphql.datafetching.ExecutionForest;
import org.hypergraphql.datafetching.ExecutionForestFactory;
import org.hypergraphql.datamodel.ModelContainer;
import org.hypergraphql.query.QueryValidator;
import org.hypergraphql.query.ValidatedQuery;

import java.util.*;

/**
 * Created by szymon on 01/11/2017.
 */
public class HGQLQueryService {

    private GraphQL graphql;
    private GraphQLSchema schema;
    private HGQLSchema hgqlSchema;


    public HGQLQueryService (HGQLConfig config) {
        this.hgqlSchema = config.getHgqlSchema();
        this.schema = config.getSchema();

        this.graphql = GraphQL.newGraphQL(config.getSchema()).build();
    }

    public Map<String, Object> results(String query, String acceptType) {

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        Map<Object, Object> extensions = new HashMap<>();
        List<GraphQLError> errors = new ArrayList<>();

        result.put("errors", errors);
        result.put("extensions", extensions);

        ExecutionInput executionInput;
        ExecutionResult qlResult = null;


        ValidatedQuery validatedQuery = new QueryValidator(schema).validateQuery(query);

        if (!validatedQuery.getValid()) {
            errors.addAll(validatedQuery.getErrors());
            return result;
        }

         if (!query.contains("IntrospectionQuery") && !query.contains("__")) {

            ExecutionForest queryExecutionForest = new ExecutionForestFactory().getExecutionForest(validatedQuery.getParsedQuery(), hgqlSchema);

            ModelContainer client = new ModelContainer(queryExecutionForest.generateModel());


            if (acceptType!=null) {
                result.put("data", client.getDataOutput(acceptType));
            } else {
                executionInput = ExecutionInput.newExecutionInput()
                        .query(query)
                        .context(client)
                        .build();

                qlResult = graphql.execute(executionInput);

                data.putAll(qlResult.getData());
                data.put("@context", queryExecutionForest.getFullLdContext());
            }

        } else {
            qlResult = graphql.execute(query);
            data.putAll(qlResult.getData());

        }

        if (qlResult!=null) {
            result.put("data", data);
            errors.addAll(qlResult.getErrors());
        }
        return result;
    }
}
