package org.hypergraphql.datafetching;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.language.*;
import org.apache.jena.rdf.model.Model;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.datafetching.services.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ExecutionTreeNode {
    private Service service; //service configuration
    private JsonNode query; //GraphQL in a basic Json format
    private String executionId; // unique identifier of this execution node
    private Map<String, ExecutionForest> childrenNodes; // succeeding executions
    private HGQLConfig config;
    private Set<String> input;



    public ExecutionTreeNode(HGQLConfig config, Field field, String nodeId) {

        this.config = config;
        this.service = config.queryFields().get(field.getName()).service();
        this.executionId = createId();
        this.childrenNodes = new HashMap<>();
        this.query = getFieldJson(field, null, nodeId);


    }

    public String toString() {
        String result = "";
        result += "ExecutionNodeId: " + this.executionId + "\n";
        result += "ServiceUrl: " + this.service.url() + "\n";
        result += "Query: " + this.query.toString() + "\n";
        result += "ChildrenNodes: \n";
        Set<String> children = this.childrenNodes.keySet();

        for (String child : children) {
            result += "\tParentMarker: " + child + "\t" + " Children execution nodes: " + this.childrenNodes.get(child).toString() + "\n";
        }

        result += "\n\n";

        return result;
    }


    public ExecutionTreeNode(HGQLConfig config, Service service, Set<Field> fields, String parentId) {

        this.config = config;
        this.service = service;
        this.executionId = createId();
        this.childrenNodes = new HashMap<>();
        this.query = getFieldsJson(fields, parentId);

    }


    public Service getService() {
        return service;
    }

    public JsonNode getQuery() {
        return query;
    }

    public String getExecutionId() {
        return executionId;
    }

    public Set<String> getInput() {
        return input;
    }




    private JsonNode getFieldsJson(Set<Field> fields, String parentId) {

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode query = mapper.createArrayNode();

        int i = 0;

        for (Field field : fields) {

            i++;
            String nodeId = parentId + "_" + i;
            query.add(getFieldJson(field, parentId, nodeId));

        }

        return query;

    }


    private JsonNode getFieldJson(Field field, String parentId, String nodeId) {

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode query = mapper.createObjectNode();

        query.put("name", field.getName());
        query.put("alias", field.getAlias());
        query.put("parentId", parentId);
        query.put("nodeId", nodeId);
        List<Argument> args = field.getArguments();

        if (!args.isEmpty()) {

            query.set("args", getArgsJson(args));

        }
        query.set("fields", this.traverse(field, nodeId));

        return query;

    }


    private JsonNode traverse(Field field, String parentId) {

        SelectionSet subFields = field.getSelectionSet();
        if (subFields!=null) {

            Map<Service, Set<Field>> splitFields = getPartitionedFields(subFields);

            Set<Service> serviceCalls = splitFields.keySet();

            for (Service serviceCall : serviceCalls) {

                if (serviceCall==this.service) {

                    Set<Field> subfields = splitFields.get(serviceCall);
                    JsonNode fields = getFieldsJson(subfields, parentId);
                    return fields;

                } else {

                    ExecutionTreeNode childNode = new ExecutionTreeNode(config, serviceCall, splitFields.get(serviceCall), parentId);

                    if (this.childrenNodes.containsKey(parentId)) {
                        try {
                            this.childrenNodes.get(parentId).add(childNode);
                        } catch (Exception e) { e.fillInStackTrace();}
                    } else {
                        ExecutionForest forest = new ExecutionForest();
                        forest.add(childNode);
                        try {
                        this.childrenNodes.put(parentId, forest);
                        } catch (Exception e) { e.fillInStackTrace();}
                    }
                }
            }
        }
        return null;
    }


    private JsonNode getArgsJson(List<Argument> args) {

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode argNode = mapper.createObjectNode();

        for (Argument arg : args) {

            Value val = arg.getValue();
            String type = val.getClass().getSimpleName();

            switch (type) {
                case "IntValue": {
                    long value = ((IntValue) val).getValue().longValueExact();
                    argNode.put(arg.getName().toString(), value);
                    break;
                }
                case "StringValue": {
                    String value = ((StringValue) val).getValue().toString();
                    argNode.put(arg.getName().toString(), value);
                    break;
                }
                case "BooleanValue": {
                    Boolean value = ((BooleanValue) val).isValue();
                    argNode.put(arg.getName().toString(), value);
                    break;
                }
            }

        }

        return argNode;
    }


    private Map<Service, Set<Field>> getPartitionedFields(SelectionSet selectionSet) {

        Map<Service, Set<Field>> result = new HashMap<>();

        List<Selection> selections = selectionSet.getSelections();

        for (Selection child : selections) {

            if (child.getClass().getSimpleName().equals("Field")) {

                Field field = (Field) child;

                if (config.fields().containsKey(field.getName())) {

                    Service serviceConfig = config.fields().get(field.getName()).service();

                    if (result.containsKey(serviceConfig)) {

                        result.get(serviceConfig).add(field);

                    } else {

                        Set<Field> newFieldSet = new HashSet<>();
                        newFieldSet.add(field);
                        result.put(serviceConfig, newFieldSet);

                    }
                }

            }

        }

        return result;
    }


    public String createId() {
        return "execution-"+ UUID.randomUUID();
    }

    public Model generateTreeModel(Set<String> input) {



        TreeExecutionResult executionResult = service.executeQuery(query, input);
        Map<String,Set<String>> resultset = executionResult.getResultSet();


        Model model = executionResult.getModel();

        Set<Model> computedModels = new HashSet<>();

        //    StoredModel.getInstance().add(model);

        Set<String> vars = resultset.keySet();

        ExecutorService executor = Executors.newFixedThreadPool(50);
        Set<Future<Model>> futuremodels = new HashSet<>();

        for (String var : vars) {

            ExecutionForest executionChildren = this.childrenNodes.get(var);

            if (executionChildren.size()>0) {

                Set<String> values = resultset.get(var);

                for (ExecutionTreeNode node : executionChildren) {


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
