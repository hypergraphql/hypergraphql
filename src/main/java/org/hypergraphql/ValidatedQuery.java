package org.hypergraphql;

import graphql.language.Document;
import graphql.validation.ValidationError;

import java.util.List;

public class ValidatedQuery {

    Document parsedQuery;
    List<ValidationError> errors;
    Boolean valid;

}
