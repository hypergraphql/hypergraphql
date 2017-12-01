package org.hypergraphql.datafetching;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
        ExecutorService executor = Executors.newFixedThreadPool(10);
        Model model = ModelFactory.createDefaultModel();
        Set<Future<Model>> futuremodels = new HashSet<>();
        for (ExecutionTreeNode node : this.forest) {
            FetchingExecution fetchingExecution = new FetchingExecution(new HashSet<String>(), node);
            futuremodels.add(executor.submit(fetchingExecution));


        }
        futuremodels.iterator().forEachRemaining(futureModel -> {
            try {
                model.add(futureModel.get());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        });
        return model;
    }

    public String toString(int i) {

        String result = "";

        for (ExecutionTreeNode node : this.forest) {
            result += node.toString(i);
        }

        return result;
    }

}
