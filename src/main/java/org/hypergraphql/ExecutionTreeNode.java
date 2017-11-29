package org.hypergraphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.language.*;
import org.hypergraphql.config.HGQLConfig;
import org.hypergraphql.config.ServiceConfig;

import java.util.*;

public class ExecutionTreeNode {
    private ServiceConfig service; //service configuration
    private JsonNode query; //GraphQL in a basic Json format
    private String executionId; // unique identifier of this execution node
    private Map<String, ExecutionForest> childrenNodes; // succeeding executions
    private HGQLConfig config;


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


    public ExecutionTreeNode(HGQLConfig config, ServiceConfig service, Set<Field> fields, String parentId) {

        this.config = config;
        this.service = service;
        this.executionId = createId();
        this.childrenNodes = new HashMap<>();
        this.query = getFieldsJson(fields, parentId);

    }


    public ServiceConfig getService() {
        return service;
    }

    public JsonNode getQuery() {
        return query;
    }

    public String getExecutionId() {
        return executionId;
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

            query.put("args", getArgsJson(args));

        }
        query.put("fields", this.traverse(field, nodeId));

        return query;

    }


    private JsonNode traverse(Field field, String parentId) {

        SelectionSet subFields = field.getSelectionSet();
        if (subFields!=null) {

            Map<ServiceConfig, Set<Field>> splitFields = getPartitionedFields(subFields);

            Set<ServiceConfig> serviceCalls = splitFields.keySet();

            for (ServiceConfig serviceCall : serviceCalls) {

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


    private Map<ServiceConfig, Set<Field>> getPartitionedFields(SelectionSet selectionSet) {

        Map<ServiceConfig, Set<Field>> result = new HashMap<>();

        List<Selection> selections = selectionSet.getSelections();

        for (Selection child : selections) {

            if (child.getClass().getSimpleName().equals("Field")) {

                Field field = (Field) child;

                if (config.fields().containsKey(field.getName())) {

                    ServiceConfig serviceConfig = config.fields().get(field.getName()).service();

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


}
