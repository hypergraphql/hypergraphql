package org.hypergraphql.datafetching;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.web.HttpOp;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.apache.log4j.Logger;
import org.hypergraphql.datafetching.services.SPARQLEndpointService;
import org.hypergraphql.datamodel.HGQLSchema;
import org.hypergraphql.query.converters.SPARQLServiceConverter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;


public class SPARQLEndpointExecution implements Callable<SPARQLExecutionResult> {

    protected JsonNode query;
    Set<String> inputSubset;
    Set<String> markers;
    SPARQLEndpointService sparqlEndpointService;
    protected HGQLSchema schema ;
    protected Logger logger = Logger.getLogger(SPARQLEndpointExecution.class);
    String rootType;

    public SPARQLEndpointExecution(JsonNode query, Set<String> inputSubset, Set<String> markers, SPARQLEndpointService sparqlEndpointService, HGQLSchema schema, String rootType) {
        this.query = query;
        this.inputSubset = inputSubset;
        this.markers = markers;
        this.sparqlEndpointService = sparqlEndpointService;
        this.schema = schema;
        this.rootType=rootType;
    }

    @Override
    public SPARQLExecutionResult call() {
        Map<String, Set<String>> resultSet = new HashMap<>();

        markers.forEach(marker -> resultSet.put(marker, new HashSet<>()));

        Model unionModel = ModelFactory.createDefaultModel();

        SPARQLServiceConverter converter = new SPARQLServiceConverter(schema);
        String sparqlQuery = converter.getSelectQuery(query, inputSubset, rootType);
        logger.debug(sparqlQuery);

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        Credentials credentials =
                new UsernamePasswordCredentials(this.sparqlEndpointService.getUser(), this.sparqlEndpointService.getPassword());
        credsProvider.setCredentials(AuthScope.ANY, credentials);
        HttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();
        HttpOp.setDefaultHttpClient(httpclient);

        ARQ.init();
        Query jenaQuery = QueryFactory.create(sparqlQuery);

        QueryEngineHTTP qEngine = QueryExecutionFactory.createServiceRequest(this.sparqlEndpointService.getUrl(), jenaQuery);
        qEngine.setClient(httpclient);

        ResultSet results = qEngine.execSelect();

        results.forEachRemaining(solution -> {
            markers.stream().filter(solution::contains).forEach(marker ->
                    resultSet.get(marker).add(solution.get(marker).asResource().getURI()));

            unionModel.add(this.sparqlEndpointService.getModelFromResults(query, solution, schema));
        });

        SPARQLExecutionResult sparqlExecutionResult = new SPARQLExecutionResult(resultSet, unionModel);
        logger.debug(sparqlExecutionResult);

        return sparqlExecutionResult;
    }

}

