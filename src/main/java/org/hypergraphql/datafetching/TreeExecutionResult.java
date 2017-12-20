package org.hypergraphql.datafetching;

import org.apache.jena.rdf.model.Model;

import java.util.Map;
import java.util.Set;

public class TreeExecutionResult {


    private Model model;

    private Map<String, Set<String>> resultSet;

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public Map<String, Set<String>> getResultSet() {
        return resultSet;
    }

    public void setResultSet(Map<String, Set<String>> resultSet) {
        this.resultSet = resultSet;
    }
}
