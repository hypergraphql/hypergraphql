package org.hypergraphql.services;

import graphql.*;
import graphql.introspection.Introspection;
import graphql.introspection.IntrospectionQuery;
import graphql.introspection.IntrospectionResultToSchema;
import graphql.schema.GraphQLObjectType;
import org.apache.log4j.Logger;
import org.hypergraphql.config.system.HGQLConfig;
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
    private HGQLConfig config;

  //  static Logger logger = Logger.getLogger(HGQLQueryService.class);

    public HGQLQueryService() {
        this.config = HGQLConfig.getInstance();
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

        ValidatedQuery validatedQuery = new QueryValidator().validateQuery(query);

        if (!validatedQuery.getValid()) {
            errors.addAll(validatedQuery.getErrors());
            return result;
        }

        if (!query.contains("IntrospectionQuery") && !query.contains("__")) {

            ExecutionForest queryExecutionForest = new ExecutionForestFactory().getExecutionForest(validatedQuery.getParsedQuery());

            ModelContainer client = new ModelContainer(queryExecutionForest.generateModel());

            if (acceptType!=null) {
                result.put("data", client.getDataOutput(acceptType));
            } else {
                executionInput = ExecutionInput.newExecutionInput()
                        .query(query)
                        .context(client)
                        .build();

                qlResult = config.graphql().execute(executionInput);

                data.putAll(qlResult.getData());
                data.put("@context", queryExecutionForest.getFullLdContext());
            }

        } else {
            qlResult = config.graphql().execute(query);
            data.putAll(qlResult.getData());

        }

        if (qlResult!=null) {
            result.put("data", data);
            errors.addAll(qlResult.getErrors());
        }
        return result;
    }
}
