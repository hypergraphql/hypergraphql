package org.hypergraphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.language.Field;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import org.hypergraphql.config.HGQLConfig;
import org.hypergraphql.config.ServiceConfig;

import java.util.*;

public class ExecutionTreeNode {
    private ServiceConfig service; //service configuration
    private JsonNode query; //GraphQL in a basic Json format
    private String executionId; // unique identifier of this execution node
    private Map<String, ExecutionForest> childrenNodes; // succeeding executions
    private HGQLConfig config;


    public ExecutionTreeNode(HGQLConfig config, Field field, String parentNode) {

        this.config = config;
        this.service = config.queryFields().get(field.getName()).service();
        this.executionId = createId();


        Set<Field> fieldSet = new HashSet<>();
        fieldSet.add(field);

        this.traverse(fieldSet, parentNode);

    }

    private void traverse(Set<Field> fields, String parentNode) {

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode query = mapper.createArrayNode();

        this.query = query;

        for (Field field : fields) {
            ObjectNode rootNode = mapper.createObjectNode();

            rootNode.put("name", field.getName());
            rootNode.put("alias", field.getAlias());

            Map<ServiceConfig, Set<Field>> splitFields = getSplitFields(field.getSelectionSet());

        }

    }

    private Map<ServiceConfig, Set<Field>> getSplitFields(SelectionSet selectionSet) {

        Map<ServiceConfig, Set<Field>> result = new HashMap<>();

        List<Selection> selections = selectionSet.getSelections();

        for (Selection child : selections) {

            if (child.getClass().getSimpleName().equals("Field")) {

                Field field = (Field) child;

                ServiceConfig serviceConfig = config.queryFields().get(field.getName()).service();

                if (result.containsKey(serviceConfig)) {

                    result.get(serviceConfig).add(field);

                } else {

                    Set<Field> newFieldSet = new HashSet<>();
                    newFieldSet.add(field);
                    result.put(serviceConfig, newFieldSet);

                }

            }

        }

        return result;
    }

    public ExecutionTreeNode(HGQLConfig config, String service, SelectionSet queryFields, Map<String, String> childrenNodesInfo) {

        this.service = config.services().get(service);
        this.executionId = createId();

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
