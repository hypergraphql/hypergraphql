package org.hypergraphql.datafetching;

import org.apache.jena.rdf.model.Model;

import java.util.Map;
import java.util.Set;

public class SPARQLExecutionResult {

   private  Map<String, Set<String>> resultSet;
   private Model model;

    public Map<String, Set<String>> getResultSet() {
        return resultSet;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public SPARQLExecutionResult(Map<String, Set<String>> resultSet, Model model) {

        this.resultSet = resultSet;
        this.model = model;
    }

    @Override
    public String toString() {

        String out = "";

        out+="RESULTS\n";
        out+="Model : \n" + this.model.toString() + "\n";
        out+="ResultSet : \n" + this.resultSet.toString();


        return out;
    }
}
