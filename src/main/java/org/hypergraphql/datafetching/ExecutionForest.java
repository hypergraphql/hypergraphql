package org.hypergraphql.datafetching;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ExecutionForest  {

    static Logger logger = Logger.getLogger(ExecutionForest.class);

    private HashSet<ExecutionTreeNode> forest;

    public ExecutionForest() {
        this.forest = new HashSet<>();
    }



    public HashSet<ExecutionTreeNode> getForest() {
        return forest;
    }

    public Model generateModel() {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        Model model = ModelFactory.createDefaultModel();
        Set<Future<Model>> futuremodels = new HashSet<>();
        for (ExecutionTreeNode node : this.forest) {
            FetchingExecution fetchingExecution = new FetchingExecution(new HashSet<String>(), node);
            futuremodels.add(executor.submit(fetchingExecution));


        }
        futuremodels.iterator().forEachRemaining(futureModel -> {
            try {
                Model futuremodel = futureModel.get();
                model.add(futuremodel);
            } catch (InterruptedException e) {
                logger.error(e);
            } catch (ExecutionException e) {
                logger.error(e);
            }
        });
        return model;
    }

    public String toString() {
        return this.toString(0);
    }

    public String toString(int i) {

        String result = "";

        for (ExecutionTreeNode node : this.forest) {
            result += node.toString(i);
        }

        return result;
    }

    public Map<String, String> getFullLdContext() {

        Map<String, String> result = new HashMap<>();

        Set<ExecutionTreeNode> children = getForest();

            for (ExecutionTreeNode child : children) {
                    result.putAll(child.getFullLdContext());
            }

        return result;

    }

}
