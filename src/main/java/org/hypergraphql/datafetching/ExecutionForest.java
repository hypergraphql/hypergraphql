package org.hypergraphql.datafetching;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import java.util.HashSet;

public class ExecutionForest  {

    private HashSet<ExecutionTreeNode> forest;

    public ExecutionForest() {
        this.forest = new HashSet<>();
    }



    public HashSet<ExecutionTreeNode> getForest() {
        return forest;
    }

    public void setForest(HashSet<ExecutionTreeNode> forest) {
        this.forest = forest;
    }

    public Model generateModel() {
        Model model = ModelFactory.createDefaultModel();
        for (ExecutionTreeNode node : this.forest) {
            model.add(node.generateTreeModel(null));

        }
        return model;
    }

    public String toString() {

        String result = "";

        for (ExecutionTreeNode node : this.forest) {
            result += node.toString();
        }

        return result;
    }

}
