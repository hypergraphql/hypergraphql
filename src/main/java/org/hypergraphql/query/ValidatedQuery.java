package org.hypergraphql.query;

import graphql.language.Document;
import graphql.validation.ValidationError;

import java.util.List;

public class ValidatedQuery {

    public Document getParsedQuery() {
        return parsedQuery;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public Boolean getValid() {
        return valid;
    }

    Document parsedQuery;
    List<ValidationError> errors;
    Boolean valid;

}
