package org.hypergraphql.datafetching;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutionForest  {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionForest.class);

    private final Set<ExecutionTreeNode> forest;

    public ExecutionForest() {
        this.forest = new HashSet<>();
    }

    public Set<ExecutionTreeNode> getForest() {
        return forest;
    }

    public Model generateModel() {

        final var executor = Executors.newFixedThreadPool(10);
        final var model = ModelFactory.createDefaultModel();
        final Set<Future<Model>> futureModels = new HashSet<>();
        getForest().forEach(node -> {
            final var fetchingExecution = new FetchingExecution(new HashSet<>(), node);
            futureModels.add(executor.submit(fetchingExecution));
        });
        futureModels.forEach(futureModel -> {
            try {
                model.add(futureModel.get());
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error("Problem generating model", e);
            }
        });
        return model;
    }

    public String toString() {
        return this.toString(0);
    }

    public String toString(int i) {

        final var result = new StringBuilder();
        getForest().forEach(node -> result.append(node.toString(i)));
        return result.toString();
    }

    public Map<String, String> getFullLdContext() {

        final Map<String, String> result = new HashMap<>();
        getForest().forEach(child -> result.putAll(child.getFullLdContext()));
        return result;
    }

}
