package org.hypergraphql.datafetching;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

@Slf4j
@Getter
public class ExecutionForest  {

    private static final int THREAD_POOL_SIZE = 10; // TODO - make this configurable
    private final Set<ExecutionTreeNode> forest;

    public ExecutionForest() {
        this.forest = new HashSet<>();
    }

    public Model generateModel() {

        val executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        val model = ModelFactory.createDefaultModel();
        final Set<Future<Model>> futureModels = new HashSet<>();
        getForest().forEach(node -> {
            val fetchingExecution = new FetchingExecution(new HashSet<>(), node);
            futureModels.add(executor.submit(fetchingExecution));
        });
        futureModels.forEach(futureModel -> {
            try {
                model.add(futureModel.get());
            } catch (InterruptedException | ExecutionException e) {
                log.error("Problem generating model", e);
            }
        });
        return model;
    }

    public String toString() {
        return this.toString(0);
    }

    public String toString(int i) {

        val result = new StringBuilder();
        getForest().forEach(node -> result.append(node.toString(i)));
        return result.toString();
    }

    public Map<String, String> getFullLdContext() {

        final Map<String, String> result = new HashMap<>();
        getForest().forEach(child -> result.putAll(child.getFullLdContext()));
        return result;
    }
}
