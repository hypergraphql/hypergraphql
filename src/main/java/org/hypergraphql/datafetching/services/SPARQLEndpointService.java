package org.hypergraphql.datafetching.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.hypergraphql.config.schema.*;
import org.hypergraphql.config.system.ServiceConfig;
import org.hypergraphql.datafetching.SPARQLEndpointExecution;
import org.hypergraphql.datafetching.SPARQLExecutionResult;
import org.hypergraphql.datafetching.TreeExecutionResult;
import org.hypergraphql.datamodel.HGQLSchemaWiring;

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
    private HGQLSchemaWiring config = HGQLSchemaWiring.getInstance();

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public SPARQLEndpointService() {

    }

    @Override
    public TreeExecutionResult executeQuery(JsonNode query, Set<String> input,  Set<String> markers) {

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
                model.add(getModelFromResults(currentNode.get("getFields"), results));

            }
        }

        else {

            Model currentModel = buildmodel(results,query);
            model.add(currentModel);
            model.add(getModelFromResults(query.get("getFields"), results));

        }

        return model;

    }

    private Model buildmodel(QuerySolution results, JsonNode currentNode) {

        Model model = ModelFactory.createDefaultModel();

        FieldConfig propertyString = this.config.getFields().get(currentNode.get("name").asText());
        TypeConfig targetTypeString = this.config.getTypes().get(currentNode.get("targetName").asText());

        if (propertyString != null && !(currentNode.get("parentId").asText().equals("null"))) {
            Property predicate = model.createProperty("", propertyString.id());
            Resource subject = results.getResource(currentNode.get("parentId").asText());
            RDFNode object = results.get(currentNode.get("nodeId").asText());
            if (predicate!=null&&subject!=null&&object!=null)
            model.add(subject, predicate, object);
        }

        if (targetTypeString != null) {
            Resource subject = results.getResource(currentNode.get("nodeId").asText());
            Resource object = model.createResource(targetTypeString.getId());
            if (subject!=null&&object!=null)
            model.add(subject, RDF.type, object);
        }

        QueryFieldConfig queryField = this.config.getQueryFields().get(currentNode.get("name").asText());

        if (queryField!=null) {

            String typeName = (currentNode.get("alias").isNull()) ? currentNode.get("name").asText() : currentNode.get("alias").asText();
            Resource object = results.getResource(currentNode.get("nodeId").asText());
            Resource subject = model.createResource(HGQLVocabulary.HGQL_QUERY_URI);
            Property predicate = model.createProperty("", HGQLVocabulary.HGQL_QUERY_NAMESPACE + typeName);
            model.add(subject, predicate, object);
        }
        return model;
    }

    @Override
    public void setParameters(ServiceConfig serviceConfig) {

        super.setParameters(serviceConfig);

        this.id = serviceConfig.getId();
        this.url = serviceConfig.getUrl();
        this.user = serviceConfig.getUser();
        this.graph = serviceConfig.getGraph();
        this.password = serviceConfig.getPassword();

    }
}
