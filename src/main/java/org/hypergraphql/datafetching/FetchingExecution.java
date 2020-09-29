package org.hypergraphql.datafetching;

import org.apache.jena.rdf.model.Model;

import java.util.Set;
import java.util.concurrent.Callable;

public class FetchingExecution implements Callable<Model> {

    private final Set<String> inputValues;
    private final ExecutionTreeNode node;

    public FetchingExecution(final Set<String> inputValues, final ExecutionTreeNode node) {

        this.inputValues = inputValues;
        this.node = node;
    }
    
    @Override
    public Model call() {
        return node.generateTreeModel(inputValues);
    }
}
