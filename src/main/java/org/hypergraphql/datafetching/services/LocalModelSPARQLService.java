package org.hypergraphql.datafetching.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.hypergraphql.config.system.ServiceConfig;
import org.hypergraphql.datafetching.LocalSPARQLExecution;
import org.hypergraphql.datafetching.SPARQLExecutionResult;
import org.hypergraphql.datafetching.TreeExecutionResult;
import org.hypergraphql.datamodel.HGQLSchema;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class LocalModelSPARQLService extends SPARQLEndpointService{


    protected Model model;

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
            LocalSPARQLExecution execution = new LocalSPARQLExecution(query,inputSubset,markers,this, schema , this.model, rootType);
            futureSPARQLresults.add(executor.submit(execution));

        } while (inputList.size()>VALUES_SIZE_LIMIT);

        iterateFutureResults(futureSPARQLresults, unionModel, resultSet);

        TreeExecutionResult treeExecutionResult = new TreeExecutionResult();
        treeExecutionResult.setResultSet(resultSet);
        treeExecutionResult.setModel(unionModel);

        return treeExecutionResult;
    }

    @Override
    public void setParameters(ServiceConfig serviceConfig) {
        super.setParameters(serviceConfig);
        this.id = serviceConfig.getId();
        String filetype = serviceConfig.getFiletype();
        String filepath = serviceConfig.getFilepath();
        this.model = ModelFactory.createDefaultModel();
        this.model.read(filepath, filetype);
    }
}