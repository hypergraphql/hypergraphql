package org.hypergraphql.datafetching;

import java.util.Collection;
import java.util.Map;
import lombok.Data;
import org.apache.jena.rdf.model.Model;

@Data
public class TreeExecutionResult {

    private Model model;
    private Map<String, Collection<String>> resultSet;
}
