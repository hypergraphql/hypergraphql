package org.hypergraphql.query;

import graphql.language.Document;
import graphql.validation.ValidationError;

import java.util.List;

public class ValidatedQuery {

    public Document getParsedQuery() {
        return parsedQuery;
    }

    public void setParsedQuery(Document parsedQuery) {
        this.parsedQuery = parsedQuery;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public void setErrors(List<ValidationError> errors) {
        this.errors = errors;
    }

    public Boolean getValid() {
        return valid;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
    }

    Document parsedQuery;
    List<ValidationError> errors;
    Boolean valid;

}
