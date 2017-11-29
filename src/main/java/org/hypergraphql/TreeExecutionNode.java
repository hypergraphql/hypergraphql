package org.hypergraphql;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.jena.rdf.model.Model;
import org.hypergraphql.config.FetchingExecution;
import org.hypergraphql.config.Service;
import org.hypergraphql.config.TreeExecutionResult;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TreeExecutionNode {
    private Service service; //service configuration
    private JsonNode query; //GraphQL in a basic Json format
    private String executionId; // unique identifier of this execution node
    private Map<String, Set<TreeExecutionNode>> childrenNodes; // succeeding executions
    private Set<String> input;

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public JsonNode getQuery() {
        return query;
    }

    public void setQuery(JsonNode query) {
        this.query = query;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }


    public Map<String, Set<TreeExecutionNode>> getChildrenNodes() {
        return childrenNodes;
    }

    public void setChildrenNodes(Map<String, Set<TreeExecutionNode>> childrenNodes) {
        this.childrenNodes = childrenNodes;
    }

    public Set<String> getInput() {
        return input;
    }

    public void setInput(Set<String> input) {
        this.input = input;
    }

    public Model generateTreeModel( Set<String> input) {


        //todo
        TreeExecutionResult executionResult = service.executeQuery(query, input);
        Map<String,Set<String>> resultset = executionResult.getResultSet();


        Model model = executionResult.getModel();

        Set<Model> computedModels = new HashSet<>();

    //    StoredModel.getInstance().add(model);

        Set<String> vars = resultset.keySet();

        ExecutorService executor = Executors.newFixedThreadPool(50);
        Set<Future<Model>> futuremodels = new HashSet<>();

        for (String var : vars) {

            Set<TreeExecutionNode> executionChildren = this.childrenNodes.get(var);

            if (executionChildren.size()>0) {

                Set<String> values = resultset.get(var);

                for (TreeExecutionNode node : executionChildren) {


                    FetchingExecution childExecution = new FetchingExecution(values,node);

                    futuremodels.add(executor.submit(childExecution));

//                    Thread thread = new Thread(childExecution, node.toString());
//
//                    thread.run();


       //             node.generateTreeModel(values);

                }
            }
        }

        for (Future<Model> futureModel : futuremodels) {
            try {
                computedModels.add(futureModel.get());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        for (Model computedmodel : computedModels) {

            model.add(computedmodel);
        }

        return model;


    }


}
