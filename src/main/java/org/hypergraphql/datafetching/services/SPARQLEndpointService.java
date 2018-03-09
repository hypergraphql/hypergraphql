package org.hypergraphql.datafetching.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.hypergraphql.config.schema.HGQLVocabulary;
import org.hypergraphql.config.system.ServiceConfig;
import org.hypergraphql.datafetching.SPARQLEndpointExecution;
import org.hypergraphql.datafetching.SPARQLExecutionResult;
import org.hypergraphql.datafetching.TreeExecutionResult;
import org.hypergraphql.datamodel.HGQLSchema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SPARQLEndpointService extends SPARQLService {

    private String url;
    private String user;
    private String password;
    final static int VALUES_SIZE_LIMIT = 100;

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public TreeExecutionResult executeQuery(JsonNode query, Set<String> input, Set<String> markers , String rootType , HGQLSchema schema) {


        Map<String, Set<String>> resultSet = new HashMap<>();
        Model unionModel = ModelFactory.createDefaultModel();
        Set<Future<SPARQLExecutionResult>> futureSPARQLresults = new HashSet<>();

        List<String> inputList = getStrings(query, input, markers, rootType, schema, resultSet);

        do {

            Set<String> inputSubset = new HashSet<>();
            int i = 0;
            while (i < VALUES_SIZE_LIMIT && !inputList.isEmpty()) {
                inputSubset.add(inputList.get(0));
                inputList.remove(0);
                i++;
            }
            ExecutorService executor = Executors.newFixedThreadPool(50);
            SPARQLEndpointExecution execution = new SPARQLEndpointExecution(query,inputSubset,markers,this, schema, rootType);
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

    protected List<String> getStrings(JsonNode query, Set<String> input, Set<String> markers, String rootType, HGQLSchema schema, Map<String, Set<String>> resultSet) {
        for (String marker : markers) {
            resultSet.put(marker, new HashSet<>());
        }

        if (rootType.equals("Query")&&schema.getQueryFields().get(query.get("name").asText()).type().equals(HGQLVocabulary.HGQL_QUERY_GET_BY_ID_FIELD)) {
            Iterator<JsonNode> uris = query.get("args").get("uris").elements();
            while (uris.hasNext()) {
                String uri = uris.next().asText();
                input.add(uri);
            }
        }
        return new ArrayList<>(input);
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
