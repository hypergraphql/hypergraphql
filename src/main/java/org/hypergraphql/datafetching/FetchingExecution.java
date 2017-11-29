package org.hypergraphql.datafetching;

import org.apache.jena.rdf.model.Model;

import java.util.Set;
import java.util.concurrent.Callable;

public class FetchingExecution implements Callable<Model> {

    private Set<String> inputValues;
    private ExecutionTreeNode node;

    public Set<String> getInputValues() {
        return inputValues;
    }

    public void setInputValues(Set<String> inputValues) {
        this.inputValues = inputValues;
    }

    public ExecutionTreeNode getNode() {
        return node;
    }

    public void setNode(ExecutionTreeNode node) {
        this.node = node;
    }

    public FetchingExecution(Set<String> inputValues, ExecutionTreeNode node) {

        this.inputValues = inputValues;
        this.node = node;
    }



    @Override
    public Model call() throws Exception {
        return node.generateTreeModel(inputValues);
    }
}
