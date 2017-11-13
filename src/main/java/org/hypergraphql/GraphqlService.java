package org.hypergraphql;

import com.fasterxml.jackson.databind.JsonNode;
import graphql.*;
import graphql.execution.NonNullableFieldWasNullError;
import graphql.language.Document;
import graphql.language.SourceLocation;
import graphql.parser.Parser;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.errors.NotAnOutputTypeError;
import graphql.validation.ValidationError;
import graphql.validation.ValidationErrorType;
import graphql.validation.Validator;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by szymon on 01/11/2017.
 */
public class GraphqlService {
    private GraphQL graphQL;
    private Config config;
    static Logger logger = Logger.getLogger(GraphqlService.class);


    public GraphqlService(Config config, GraphQL graphQL) {
        this.graphQL = graphQL;
        this.config = config;
    }

    public Map<String, Object> results(String query, GraphQLSchema schema) {

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        Map<Object, Object> extensions = new HashMap<>();
        List<GraphQLError> errors = new ArrayList<>();

        ExecutionInput executionInput;
        ExecutionResult qlResult;

        List<Map<String, String>> sparqlQueries;

        List<ValidationError> validationErrors = null;


            Validator validator = new Validator();
            Parser parser = new Parser();
         Document document;
        try {
             document = parser.parseDocument(query);
        } catch (Exception e) {
            GraphQLError err = new ValidationError(ValidationErrorType.InvalidSyntax, new SourceLocation(0, 0), "unrecognized symbols");
            errors.add(err);
            result.put("errors", errors);
            return result;
        }

        validationErrors = validator.validateDocument(schema, document);
            if (validationErrors.size() > 0) {
                errors.addAll(validationErrors);
                result.put("errors", errors);
                return result;
            }



        if (!query.contains("IntrospectionQuery") && !query.contains("__")) {

            Converter converter = new Converter(config);
            JsonNode jsonQuery;
            try {
                jsonQuery = converter.query2json(query);

                sparqlQueries = converter.graphql2sparql(converter.includeContextInQuery(jsonQuery));
            } catch (Exception e) {
                GraphQLError err = new ValidationError(ValidationErrorType.InvalidSyntax, new SourceLocation(0, 0),
                        "Queries of this form are not yet supported by HyperGraphQL.");
                errors.add(err);
                result.put("errors", errors);
                return result;
            }


            // uncomment this lines if you want to include the generated SPARQL queries in the GraphQL response for debugging purposes
            // extensions.put("sparqlQueries", sparqlQueries);

            logger.info("Generated SPARQL queries:");
            logger.info(sparqlQueries.toString());

            SparqlClient client = new SparqlClient(sparqlQueries, config);

            executionInput = ExecutionInput.newExecutionInput()
                    .query(query)
                    .context(client)
                    .build();

            qlResult = graphQL.execute(executionInput);
            try {
                data = converter.jsonLDdata(qlResult.getData(), jsonQuery);
            } catch (IOException e) {
                logger.error(e);
            }

        } else {
            qlResult = graphQL.execute(query);
            data = qlResult.getData();
        }

        errors.addAll(qlResult.getErrors());

        result.put("data", data);
        result.put("errors", errors);
        result.put("extensions", extensions);

        return result;
    }
}
