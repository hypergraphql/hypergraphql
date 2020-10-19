package org.hypergraphql.query;

import graphql.language.Document;
import graphql.validation.ValidationError;
import java.util.List;
import lombok.Data;

@Data
public class ValidatedQuery {

    private Document parsedQuery;
    private List<ValidationError> errors;
    private Boolean valid;
}
