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
import org.antlr.v4.runtime.misc.ParseCancellationException;

public class QueryValidator {

    private final GraphQLSchema schema;
    private final List<ValidationError> validationErrors;
    private final Validator validator;
    private final Parser parser;

    public QueryValidator(final GraphQLSchema schema) {

        this.schema = schema;
        this.validationErrors = new ArrayList<>();
        this.validator = new Validator();
        this.parser = new Parser();
    }

    public ValidatedQuery validateQuery(final String query) {

        final ValidatedQuery result = new ValidatedQuery();
        result.setErrors(validationErrors);

        final Document document;

        try {
            document = parser.parseDocument(query);
            result.setParsedQuery(document);
        } catch (ParseCancellationException e) {
            final ValidationError err =
                    new ValidationError(ValidationErrorType.InvalidSyntax, new SourceLocation(0, 0), "Invalid query syntax.");
            validationErrors.add(err);
            result.setValid(false);
            return result;
        }

        validationErrors.addAll(validator.validateDocument(schema, document));
        result.setValid(validationErrors.size() == 0);
//        if (validationErrors.size() > 0) {
//            result.setValid(false);
//            return result;
//        }
//        result.setValid(true);
        return result;
    }
}
