package org.hypergraphql.datafetching.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.hypergraphql.config.schema.FieldConfig;
import org.hypergraphql.config.schema.HGQLVocabulary;
import org.hypergraphql.config.schema.QueryFieldConfig;
import org.hypergraphql.config.schema.TypeConfig;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.datafetching.SPARQLEndpointExecution;
import org.hypergraphql.datafetching.SPARQLExecutionResult;
import org.hypergraphql.datafetching.TreeExecutionResult;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SPARQLEndpointService extends SPARQLService {

    private String url;
    private String user;
    private String password;
    private int VALUES_SIZE_LIMIT = 100;
    private HGQLConfig config;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public SPARQLEndpointService() {

    }

    @Override
    public TreeExecutionResult executeQuery(JsonNode query, Set<String> input, String rootType, Set<String> markers) {

        Map<String, Set<String>> resultSet = new HashMap<>();

        Model unionModel = ModelFactory.createDefaultModel();

        Set<Future<SPARQLExecutionResult>> futureSPARQLresults = new HashSet<>();

        for (String marker : markers) {
            resultSet.put(marker, new HashSet<>());
        }

        List<String> inputList = (List<String>) new ArrayList(input);

        do {

            Set<String> inputSubset = new HashSet<>();
            int i = 0;
            while (i < VALUES_SIZE_LIMIT && !inputList.isEmpty()) {
                inputSubset.add(inputList.get(0));
                inputList.remove(0);
                i++;
            }
            ExecutorService executor = Executors.newFixedThreadPool(50);



            SPARQLEndpointExecution execution = new SPARQLEndpointExecution(query,inputSubset,markers,this);
            futureSPARQLresults.add(executor.submit(execution));





        } while (inputList.size()>VALUES_SIZE_LIMIT);

        for (Future<SPARQLExecutionResult> futureexecutionResult : futureSPARQLresults) {

            try {
                SPARQLExecutionResult result = futureexecutionResult.get();
                unionModel.add(result.getModel());
                resultSet.putAll(result.getResultSet());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        TreeExecutionResult treeExecutionResult = new TreeExecutionResult();

        treeExecutionResult.setResultSet(resultSet);
        treeExecutionResult.setModel(unionModel);

        return treeExecutionResult;
    }

    public Model getModelFromResults(JsonNode query, QuerySolution results) {

        Model model = ModelFactory.createDefaultModel();
        if (query.isNull()) return model;

        if (query.isArray()) {


            Iterator<JsonNode> nodesIterator = query.elements();

            while (nodesIterator.hasNext()) {


                JsonNode currentNode = nodesIterator.next();

                Model currentmodel = buildmodel(results, currentNode);
                model.add(currentmodel);
                model.add(getModelFromResults(currentNode.get("fields"), results));

            }
        }

        else {

            Model currentModel = buildmodel(results,query);
            model.add(currentModel);
            model.add(getModelFromResults(query.get("fields"), results));

        }

        return model;

    }

    private Model buildmodel(QuerySolution results, JsonNode currentNode) {


        this.config = HGQLConfig.getInstance();

        Model model = ModelFactory.createDefaultModel();

        FieldConfig propertyString = this.config.fields().get(currentNode.get("name").asText());
        TypeConfig targetTypeString = this.config.types().get(currentNode.get("targetName").asText());

        if (propertyString != null && !(currentNode.get("parentId").asText().equals("null"))) {
            Property predicate = model.createProperty("", propertyString.id());
            Resource subject = results.getResource(currentNode.get("parentId").asText());
            RDFNode object = results.get(currentNode.get("nodeId").asText());
            if (predicate!=null&&subject!=null&&object!=null)
            model.add(subject, predicate, object);
        }

        if (targetTypeString != null) {
            Resource subject = results.getResource(currentNode.get("nodeId").asText());
            Resource object = model.createResource(targetTypeString.id());
            if (subject!=null&&object!=null)
            model.add(subject, RDF.type, object);
        }

        QueryFieldConfig queryField = this.config.queryFields().get(currentNode.get("name").asText());

        if (queryField!=null) {

            String typeName = (currentNode.get("alias").isNull()) ? currentNode.get("name").asText() : currentNode.get("alias").asText();
            Resource object = results.getResource(currentNode.get("nodeId").asText());
            Resource subject = model.createResource(HGQLVocabulary.HGQL_QUERY_URI);
            Property predicate = model.createProperty("", HGQLVocabulary.HGQL_QUERY_PREFIX + typeName);
            model.add(subject, predicate, object);
        }
        return model;
    }

    @Override
    public void setParameters(JsonNode jsonnode) {

        super.setParameters(jsonnode);

        this.id = jsonnode.get("id").asText();
        this.url = jsonnode.get("url").asText();
        this.user = jsonnode.get("user").asText();
        this.password = jsonnode.get("password").asText();

    }
}
