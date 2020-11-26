package org.hypergraphql.datafetching.services;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.Getter;
import lombok.val;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.hypergraphql.config.schema.HGQLVocabulary;
import org.hypergraphql.config.system.ServiceConfig;
import org.hypergraphql.datafetching.SPARQLEndpointExecution;
import org.hypergraphql.datafetching.SPARQLExecutionResult;
import org.hypergraphql.datafetching.TreeExecutionResult;
import org.hypergraphql.datamodel.HGQLSchema;

import static org.hypergraphql.util.HGQLConstants.ARGS;
import static org.hypergraphql.util.HGQLConstants.NAME;
import static org.hypergraphql.util.HGQLConstants.URIS;

@Getter
public class SPARQLEndpointService extends SPARQLService {

    public static final int VALUES_SIZE_LIMIT = 100;
    private static final int THREAD_POOL_SIZE = 50;
    private String url;
    private String user;
    private String password;

    @Override
    public TreeExecutionResult executeQuery(
            final JsonNode query,
            final Collection<String> input,
            final Collection<String> markers,
            final String rootType,
            final HGQLSchema schema
    ) {

        final Map<String, Collection<String>> resultSet = new HashMap<>();
        val unionModel = ModelFactory.createDefaultModel();
        final Collection<Future<SPARQLExecutionResult>> futureSPARQLresults = new HashSet<>();
        final List<String> inputList = getStrings(query, input, markers, rootType, schema, resultSet);

        do {
            final Collection<String> inputSubset = new HashSet<>();
            int i = 0;
            while (i < VALUES_SIZE_LIMIT && !inputList.isEmpty()) {
                inputSubset.add(inputList.get(0));
                inputList.remove(0);
                i++;
            }
            val executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
            val execution = buildExecutor(query, inputSubset, markers, schema, rootType);
            futureSPARQLresults.add(executor.submit(execution));
        } while (inputList.size() > VALUES_SIZE_LIMIT);
        iterateFutureResults(futureSPARQLresults, unionModel, resultSet);
        val treeExecutionResult = new TreeExecutionResult();
        treeExecutionResult.setResultSet(resultSet);
        treeExecutionResult.setModel(unionModel);

        return treeExecutionResult;
    }

    void iterateFutureResults(
            final Collection<Future<SPARQLExecutionResult>> futureSPARQLResults,
            final Model unionModel,
            final Map<String, Collection<String>> resultSet
    ) {
        for (Future<SPARQLExecutionResult> futureExecutionResult : futureSPARQLResults) {
            try {
                val result = futureExecutionResult.get();
                unionModel.add(result.getModel());
                resultSet.putAll(result.getResultSet());
            } catch (InterruptedException
                    | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    List<String> getStrings(final JsonNode query,
                            final Collection<String> input,
                            final Collection<String> markers,
                            final String rootType,
                            final HGQLSchema schema,
                            final Map<String, Collection<String>> resultSet) {
        for (final String marker : markers) {
            resultSet.put(marker, new HashSet<>());
        }

        if ("Query".equals(rootType) && schema.getQueryFields().get(query.get(NAME).asText()).type().equals(HGQLVocabulary.HGQL_QUERY_GET_BY_ID_FIELD)) {
            final Iterator<JsonNode> uris = query.get(ARGS).get(URIS).elements();
            while (uris.hasNext()) {
                String uri = uris.next().asText();
                input.add(uri);
            }
        }
        return new ArrayList<>(input);
    }

    @Override
    public void setParameters(final ServiceConfig serviceConfig) {
        super.setParameters(serviceConfig);
        setId(serviceConfig.getId());
        this.url = serviceConfig.getUrl();
        this.user = serviceConfig.getUser();
        setGraph(serviceConfig.getGraph());
        this.password = serviceConfig.getPassword();
    }

    @Override
    protected SPARQLEndpointExecution buildExecutor(
            final JsonNode query,
            final Collection<String> inputSubset,
            final Collection<String> markers,
            final HGQLSchema schema,
            final String rootType) {
        return new SPARQLEndpointExecution(query, inputSubset, markers, this, schema, rootType);
    }
}
