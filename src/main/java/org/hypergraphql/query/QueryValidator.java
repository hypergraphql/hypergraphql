package org.hypergraphql.query;

import graphql.language.Document;
import graphql.language.SourceLocation;
import graphql.parser.Parser;
import graphql.schema.GraphQLSchema;
import graphql.validation.ValidationError;
import graphql.validation.ValidationErrorType;
import graphql.validation.Validator;

import java.util.ArrayList;
import java.util.List;

public class QueryValidator {

    private GraphQLSchema schema;
    private List<ValidationError> validationErrors;
    private Validator validator;
    private Parser parser;

    public QueryValidator(GraphQLSchema schema) {

        this.schema = schema;
        this.validationErrors = new ArrayList<>();
        this.validator = new Validator();
        this.parser = new Parser();

    }


    public ValidatedQuery validateQuery(String query) {

        ValidatedQuery result = new ValidatedQuery();
        result.errors = validationErrors;

        Document document;

        try {

            document = parser.parseDocument(query);
            result.parsedQuery = document;

        } catch (Exception e) {
            ValidationError err = new ValidationError(ValidationErrorType.InvalidSyntax, new SourceLocation(0, 0), "Invalid query syntax.");
            validationErrors.add(err);
            result.valid = false;

            return result;
        }

        validationErrors.addAll(validator.validateDocument(schema, document));
        if (validationErrors.size() > 0) {
            result.valid = false;

            return result;

        }

        result.valid = true;

        return result;

    }

}
