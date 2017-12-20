package org.hypergraphql.datafetching;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.language.*;
import org.apache.jena.rdf.model.Model;
import org.apache.log4j.Logger;
import org.hypergraphql.config.schema.FieldOfTypeConfig;
import org.hypergraphql.datamodel.HGQLSchema;
import org.hypergraphql.config.schema.HGQLVocabulary;
import org.hypergraphql.datafetching.services.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ExecutionTreeNode {

    private Service service; //getSetvice configuration
    private JsonNode query; //GraphQL in a basic Json format
    private String executionId; // unique identifier of this execution node
    private Map<String, ExecutionForest> childrenNodes; // succeeding executions
    private String rootType;
    private Map<String, String> ldContext;
    private HGQLSchema hgqlSchema;

    static Logger logger = Logger.getLogger(ExecutionTreeNode.class);

    public void setService(Service service) {
        this.service = service;
    }

    public void setQuery(JsonNode query) {
        this.query = query;
    }

    public Map<String, ExecutionForest> getChildrenNodes() {
        return childrenNodes;
    }

    public String getRootType() {
        return rootType;
    }

    public Map<String, String> getLdContext() { return this.ldContext; }

    public Service getService() {
        return service;
    }

    public JsonNode getQuery() { return query; }

    public String getExecutionId() {
        return executionId;
    }


    public Map<String, String> getFullLdContext() {

        Map<String, String> result = new HashMap<>();
        result.putAll(ldContext);

        Collection<ExecutionForest> children = getChildrenNodes().values();

        if (!children.isEmpty()) {
            for (ExecutionForest child : children) {
                    result.putAll(child.getFullLdContext());
            }
        }

        return result;

    }

    public ExecutionTreeNode(Field field, String nodeId , HGQLSchema schema ) {

        this.service = schema.getQueryFields().get(field.getName()).service();
        this.executionId = createId();
        this.childrenNodes = new HashMap<>();
        this.ldContext = new HashMap<>();
        this.ldContext.putAll(HGQLVocabulary.JSONLD);
        this.rootType = "Query";
        this.hgqlSchema = schema;
        this.query = getFieldJson(field, null, nodeId, "Query");


    }

    public ExecutionTreeNode(Service service, Set<Field> fields, String parentId, String parentType, HGQLSchema schema) {

        this.service = service;
        this.executionId = createId();
        this.childrenNodes = new HashMap<>();
        this.ldContext = new HashMap<>();
        this.rootType = parentType;
        this.hgqlSchema = schema;
        this.query = getFieldsJson(fields, parentId, parentType);
        this.ldContext.putAll(HGQLVocabulary.JSONLD);

    }


    public String toString(int i) {

        String space = "";
        for (int n = 0; n<i ; n++) {
            space += "\t";
        }

        String result = "\n";
        result += space + "ExecutionNode ID: " + this.executionId + "\n";
        result += space + "Service ID: " + this.service.getId() + "\n";
        result += space + "Query: " + this.query.toString() + "\n";
        result += space + "Root type: " + this.rootType + "\n";
        result += space + "LD context: " + this.ldContext.toString() + "\n";
        Set<String> children = this.childrenNodes.keySet();
        if (!children.isEmpty()) {
            result += space + "Children nodes: \n";
            for (String child : children) {
                result += space + "\tParent marker: " + child + "\n" + space + "\tChildren execution nodes: \n" + this.childrenNodes.get(child).toString(i+1) + "\n";
            }
        }

        result += "\n";

        return result;
    }


    private JsonNode getFieldsJson(Set<Field> fields, String parentId, String parentType) {

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode query = mapper.createArrayNode();

        int i = 0;

        for (Field field : fields) {

            i++;
            String nodeId = parentId + "_" + i;
            query.add(getFieldJson(field, parentId, nodeId, parentType));

        }

        return query;

    }


    private JsonNode getFieldJson(Field field, String parentId, String nodeId, String parentType) {

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode query = mapper.createObjectNode();

        query.put("name", field.getName());
        query.put("alias", field.getAlias());
        query.put("parentId", parentId);
        query.put("nodeId", nodeId);
        List<Argument> args = field.getArguments();

        String contextLdKey = (field.getAlias()==null) ? field.getName() : field.getAlias();
        String contextLdValue = getContextLdValue(contextLdKey);

        this.ldContext.put(contextLdKey, contextLdValue);

        if (!args.isEmpty()) {

            query.set("args", getArgsJson(args));

        } else {

            query.set("args", null);

        }

        FieldOfTypeConfig fieldConfig = hgqlSchema.getTypes().get(parentType).getField(field.getName());
        String targetName = fieldConfig.getTargetName();

        query.put("targetName", targetName);

        query.set("fields", this.traverse(field, nodeId, parentType));

        return query;

    }

    private String getContextLdValue(String contextLdKey) {
        if (hgqlSchema.getFields().containsKey(contextLdKey)) {
            return hgqlSchema.getFields().get(contextLdKey).getId().toString();
        } else {
            String value = HGQLVocabulary.HGQL_QUERY_NAMESPACE + contextLdKey;
            return value;
        }
    }


    private JsonNode traverse(Field field, String parentId, String parentType) {

        SelectionSet subFields = field.getSelectionSet();
        if (subFields!=null) {

            FieldOfTypeConfig fieldConfig = hgqlSchema.getTypes().get(parentType).getField(field.getName());
            String targetName = fieldConfig.getTargetName();

            Map<Service, Set<Field>> splitFields = getPartitionedFields(targetName, subFields);

            Set<Service> serviceCalls = splitFields.keySet();


            for (Service serviceCall : serviceCalls) {
                if (serviceCall != this.service) {
                    ExecutionTreeNode childNode = new ExecutionTreeNode(serviceCall, splitFields.get(serviceCall), parentId, targetName, hgqlSchema);

                    if (this.childrenNodes.containsKey(parentId)) {
                        try {
                            this.childrenNodes.get(parentId).getForest().add(childNode);
                        } catch (Exception e) {
                            logger.error(e);
                        }
                    } else {
                        ExecutionForest forest = new ExecutionForest();
                        forest.getForest().add(childNode);
                        try {
                            this.childrenNodes.put(parentId, forest);
                        } catch (Exception e) {
                            logger.error(e);
                        }
                    }
                }
            }

            if (serviceCalls.contains(this.service)) {

                Set<Field> subfields = splitFields.get(this.service);
                JsonNode fields = getFieldsJson(subfields, parentId, targetName);
                return fields;
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
                case "ArrayValue": {
                    List<Node> nodes = val.getChildren();
                    ArrayNode arrayNode = mapper.createArrayNode();

                    for (Node node : nodes)  {
                        String value = ((StringValue) node).getValue().toString();
                        arrayNode.add(value);
                    }
                    argNode.set(arg.getName().toString(), arrayNode);
                    break;
                }
            }

        }

        return argNode;
    }


    private Map<Service, Set<Field>> getPartitionedFields(String parentType, SelectionSet selectionSet) {

        Map<Service, Set<Field>> result = new HashMap<>();

        List<Selection> selections = selectionSet.getSelections();

        for (Selection child : selections) {

            if (child.getClass().getSimpleName().equals("Field")) {

                Field field = (Field) child;

                if (hgqlSchema.getFields().containsKey(field.getName())) {

                    Service serviceConfig;

                    serviceConfig = hgqlSchema.getTypes().get(parentType).getFields().get(field.getName()).getService();


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




        TreeExecutionResult executionResult = service.executeQuery(query, input,  childrenNodes.keySet() , rootType, hgqlSchema);

        Map<String,Set<String>> resultset = executionResult.getResultSet();


        Model model = executionResult.getModel();

        Set<Model> computedModels = new HashSet<>();

        //    StoredModel.getInstance().add(model);

        Set<String> vars = resultset.keySet();

        ExecutorService executor = Executors.newFixedThreadPool(50);
        Set<Future<Model>> futuremodels = new HashSet<>();

        for (String var : vars) {

            ExecutionForest executionChildren = this.childrenNodes.get(var);

            if (executionChildren.getForest().size()>0) {

                Set<String> values = resultset.get(var);

                for (ExecutionTreeNode node : executionChildren.getForest()) {


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
                logger.error(e);
            } catch (ExecutionException e) {
                logger.error(e);
            }
        }

        for (Model computedmodel : computedModels) {

            model.add(computedmodel);
        }

        return model;


    }


}
