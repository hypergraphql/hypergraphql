package org.hypergraphql.query;

import graphql.language.Document;
import graphql.validation.ValidationError;
import java.util.List;

public class ValidatedQuery {

    private Document parsedQuery;
    private List<ValidationError> errors;
    private Boolean valid;

//    public ValidatedQuery(final Document parsedQuery,
//                          final List<ValidationError> errors,
//                          final Boolean valid) {
//        this.parsedQuery = parsedQuery;
//        this.errors = errors;
//        this.valid = valid;
//    }

    public void setParsedQuery(final Document parsedQuery) {
        this.parsedQuery = parsedQuery;
    }

    public void setErrors(final List<ValidationError> errors) {
        this.errors = errors;
    }

    public void setValid(final Boolean valid) {
        this.valid = valid;
    }

    public Document getParsedQuery() {
        return parsedQuery;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public Boolean getValid() {
        return valid;
    }

}
