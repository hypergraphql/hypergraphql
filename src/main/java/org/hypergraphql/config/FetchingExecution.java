package org.hypergraphql.config;

import org.hypergraphql.TreeExecutionNode;

import java.util.Set;

public class FetchingExecution implements Runnable{

    private Set<String> inputValues;
    private TreeExecutionNode node;

    public Set<String> getInputValues() {
        return inputValues;
    }

    public void setInputValues(Set<String> inputValues) {
        this.inputValues = inputValues;
    }

    public TreeExecutionNode getNode() {
        return node;
    }

    public void setNode(TreeExecutionNode node) {
        this.node = node;
    }

    public FetchingExecution(Set<String> inputValues, TreeExecutionNode node) {

        this.inputValues = inputValues;
        this.node = node;
    }

    @Override
    public void run() {

    }
}
