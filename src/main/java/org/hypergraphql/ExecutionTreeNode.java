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
    private Map<String, ExecutionTreeNode> childrenNodes; // succeeding executions
    private HGQLConfig config;


    public ExecutionTreeNode(HGQLConfig config, Field field, String nodeId) {

        this.config = config;
        this.service = config.queryFields().get(field.getName()).service();
        this.executionId = createId();
        this.query = getQueryJson(field, nodeId, true);

    }

    private JsonNode getQueryJson(Set<Field> fields, String parentNode, boolean isRoot) {

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode query = mapper.createArrayNode();

        for (Field field : fields) {

          //  query.add(getQueryJson(field, , isRoot));

        }

        return query;

    }

    private JsonNode getQueryJson(Field field, String nodeId, boolean isRoot) {

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode query = mapper.createObjectNode();

        query.put("name", field.getName());
        query.put("alias", field.getAlias());
        query.put("nodeId", nodeId);
        List<Argument> args = field.getArguments();

        if (!args.isEmpty()) {

            query.put("args", getArgsJson(args));

        }

        query.put("fields", this.traverse(field, nodeId));

        return query;

    }

    public ExecutionTreeNode(HGQLConfig config, String service, Set<Field> fields, String nodeId) {

        this.service = config.services().get(service);
        this.executionId = createId();

    }

    private JsonNode traverse(Field field, String parentNode) {

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode fieldQueries = mapper.createArrayNode();

        int i = 0;

        SelectionSet subFields = field.getSelectionSet();
        
        if (subFields!=null) {

            Map<ServiceConfig, Set<Field>> splitFields = getSplitFields(subFields);

            Set<ServiceConfig> serviceCalls = splitFields.keySet();

            for (ServiceConfig serviceCall : serviceCalls) {

                if (serviceCall==this.service) {

                    Set<Field> subfields = splitFields.get(serviceCall);

                    for (Field subfield : subfields) {

                        i++;

                        String nodeId = parentNode + "_" + i;

                        fieldQueries.add(traverse(subfield, nodeId));

                    }

                } else {

                    new ExecutionTreeNode(config, serviceCall, splitFields.get(serviceCall), );

                }
            }
        }

        return fieldQueries;
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

    private Map<ServiceConfig, Set<Field>> getSplitFields(SelectionSet selectionSet) {

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

    public ServiceConfig getService() {
        return service;
    }

    public void setService(ServiceConfig service) {
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


}
