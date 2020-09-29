package org.hypergraphql.datafetching;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.query.ARQ;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.web.HttpOp;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.hypergraphql.datafetching.services.SPARQLEndpointService;
import org.hypergraphql.datamodel.HGQLSchema;
import org.hypergraphql.query.converters.SPARQLServiceConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

// Performs HTTP query to remote store
public class SPARQLEndpointExecution implements Callable<SPARQLExecutionResult> {

    protected final JsonNode query;
    protected final Set<String> inputSubset;
    protected final Set<String> markers;
    protected final SPARQLEndpointService sparqlEndpointService;
    protected final HGQLSchema schema ;
    protected final Logger logger = LoggerFactory.getLogger(SPARQLEndpointExecution.class);
    protected final String rootType;

    public SPARQLEndpointExecution(final JsonNode query,
                                   final Set<String> inputSubset,
                                   final Set<String> markers,
                                   final SPARQLEndpointService sparqlEndpointService,
                                   final HGQLSchema schema,
                                   final String rootType) {
        this.query = query;
        this.inputSubset = inputSubset;
        this.markers = markers;
        this.sparqlEndpointService = sparqlEndpointService;
        this.schema = schema;
        this.rootType=rootType;
    }

    @Override
    public SPARQLExecutionResult call() {
        final Map<String, Set<String>> resultSet = new HashMap<>();
        markers.forEach(marker -> resultSet.put(marker, new HashSet<>()));
        final var unionModel = ModelFactory.createDefaultModel();
        final var converter = new SPARQLServiceConverter(schema);
        final var sparqlQuery = converter.getSelectQuery(query, inputSubset, rootType);
        logger.debug(sparqlQuery);

        final var credsProvider = new BasicCredentialsProvider();
        final var credentials =
                new UsernamePasswordCredentials(this.sparqlEndpointService.getUser(), this.sparqlEndpointService.getPassword());
        credsProvider.setCredentials(AuthScope.ANY, credentials);
        final var httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();
        HttpOp.setDefaultHttpClient(httpclient);

        ARQ.init();
        final var jenaQuery = QueryFactory.create(sparqlQuery);

        final var qEngine = QueryExecutionFactory.createServiceRequest(this.sparqlEndpointService.getUrl(), jenaQuery);
        qEngine.setClient(httpclient);
        //qEngine.setSelectContentType(ResultsFormat.FMT_RS_XML.getSymbol());

        final var results = qEngine.execSelect();

        results.forEachRemaining(solution -> {
            markers.stream().filter(solution::contains).forEach(marker ->
                    resultSet.get(marker).add(solution.get(marker).asResource().getURI()));

            unionModel.add(this.sparqlEndpointService.getModelFromResults(query, solution, schema));
        });

        final var sparqlExecutionResult = new SPARQLExecutionResult(resultSet, unionModel);
        logger.debug("Result: {}", sparqlExecutionResult);

        return sparqlExecutionResult;
    }

}

