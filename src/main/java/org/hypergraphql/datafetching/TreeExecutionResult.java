package org.hypergraphql.datafetching;

import java.util.Collection;
import java.util.Map;
import org.apache.jena.rdf.model.Model;

public class TreeExecutionResult {

    private Model model;

    private Map<String, Collection<String>> resultSet;

    public Model getModel() {
        return model;
    }

    public void setModel(final Model model) {
        this.model = model;
    }

    public Map<String, Collection<String>> getResultSet() {
        return resultSet;
    }

    public void setResultSet(final Map<String, Collection<String>> resultSet) {
        this.resultSet = resultSet;
    }
}
