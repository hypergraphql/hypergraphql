package org.hypergraphql;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.resultset.ResultSetMem;
import org.hypergraphql.config.FetchingExecution;
import org.hypergraphql.config.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        ResultSet resultset = service.executeQuery(query, input);
        ResultSetMem resultSetRew = new ResultSetMem(resultset) ;

        Model model = buildCurrentModel(resultset);

        List<String> vars = resultSetRew.getResultVars();

        for (String var : vars) {

            Set<TreeExecutionNode> executionChildren = this.childrenNodes.get(var);

            if (executionChildren.size()>0) {

                Set<String> values = new HashSet<String>();

                while (resultSetRew.hasNext()) {
                    values.add(resultSetRew.nextSolution().get(var).toString());
                    resultSetRew.next();

                }
                resultSetRew.rewind();
                for (TreeExecutionNode node : executionChildren) {

//todo with multithreading
//                    FetchingExecution childExecution = new FetchingExecution(values,node);
//
//                    Thread thread = new Thread(childExecution, node.toString());


                    model.add( node.generateTreeModel(values));

                }
            }
        }

        return model;
    }

    private Model buildCurrentModel(ResultSet resultset) {

        //todo
        return null;
    }
}
