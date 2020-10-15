package org.hypergraphql.datafetching;

import java.util.Map;
import java.util.Set;
import org.apache.jena.rdf.model.Model;

public class SPARQLExecutionResult {

    private final Map<String, Set<String>> resultSet;
    private Model model;

    public SPARQLExecutionResult(final Map<String, Set<String>> resultSet, final Model model) {

        this.resultSet = resultSet;
        this.model = model;
    }

    public Map<String, Set<String>> getResultSet() {
        return resultSet;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(final Model model) {
        this.model = model;
    }

    @Override
    public String toString() {

        return "RESULTS\n"
                + "Model : \n" + this.model.toString() + "\n"
                + "ResultSet : \n" + this.resultSet.toString();
    }
}
