package org.hypergraphql.datafetching;

import java.util.Map;
import java.util.Set;
import org.apache.jena.rdf.model.Model;

public class TreeExecutionResult {

    private Model model;

    private Map<String, Set<String>> resultSet;

    public Model getModel() {
        return model;
    }

    public void setModel(final Model model) {
        this.model = model;
    }

    public Map<String, Set<String>> getResultSet() {
        return resultSet;
    }

    public void setResultSet(final Map<String, Set<String>> resultSet) {
        this.resultSet = resultSet;
    }
}
