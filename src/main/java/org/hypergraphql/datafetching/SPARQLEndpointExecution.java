package org.hypergraphql.datafetching;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Callable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.query.ARQ;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.web.HttpOp;
import org.hypergraphql.datafetching.services.SPARQLEndpointService;
import org.hypergraphql.datamodel.HGQLSchema;
import org.hypergraphql.query.converters.SPARQLServiceConverter;

// Performs HTTP query to remote store
@Slf4j
@Getter
@RequiredArgsConstructor
public class SPARQLEndpointExecution implements Callable<SPARQLExecutionResult> {

    private final JsonNode query;
    private final Collection<String> inputSubset;
    private final Collection<String> markers;
    private final SPARQLEndpointService sparqlEndpointService;
    private final HGQLSchema schema;
    private final String rootType;

    @Override
    public SPARQLExecutionResult call() {
        final Map<String, Collection<String>> resultSet = new HashMap<>();
        markers.forEach(marker -> resultSet.put(marker, new HashSet<>()));
        final var unionModel = ModelFactory.createDefaultModel();
        final var converter = new SPARQLServiceConverter(schema);
        final var sparqlQuery = converter.getSelectQuery(query, inputSubset, rootType);
        log.debug(sparqlQuery);

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
        log.debug("Result: {}", sparqlExecutionResult);

        return sparqlExecutionResult;
    }
}

