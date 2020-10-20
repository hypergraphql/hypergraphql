package org.hypergraphql.datafetching;

import java.util.Collection;
import java.util.concurrent.Callable;
import org.apache.jena.rdf.model.Model;

public class FetchingExecution implements Callable<Model> {

    private final Collection<String> inputValues;
    private final ExecutionTreeNode node;

    public FetchingExecution(final Collection<String> inputValues, final ExecutionTreeNode node) {

        this.inputValues = inputValues;
        this.node = node;
    }

    @Override
    public Model call() {
        return node.generateTreeModel(inputValues);
    }
}
