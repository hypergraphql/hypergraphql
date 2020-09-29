package org.hypergraphql.datafetching;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.hypergraphql.datafetching.services.SPARQLEndpointService;
import org.hypergraphql.datamodel.HGQLSchema;
import org.hypergraphql.query.converters.SPARQLServiceConverter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LocalSPARQLExecution extends SPARQLEndpointExecution {

    private Model model;


    public LocalSPARQLExecution(final JsonNode query,
                                final Set<String> inputSubset,
                                final Set<String> markers,
                                final SPARQLEndpointService sparqlEndpointService,
                                final HGQLSchema schema,
                                final Model localModel,
                                final String rootType) {
        super(query, inputSubset, markers, sparqlEndpointService, schema, rootType);
        this.model = localModel;
    }

    @Override
    public SPARQLExecutionResult call() {

        final Map<String, Set<String>> resultSet = new HashMap<>();
        markers.forEach(marker -> resultSet.put(marker, new HashSet<>()));

        final var unionModel = ModelFactory.createDefaultModel();
        final var converter = new SPARQLServiceConverter(schema);
        final var sparqlQuery = converter.getSelectQuery(query, inputSubset, rootType);
        logger.debug(sparqlQuery);
        final var jenaQuery = QueryFactory.create(sparqlQuery);
        final var qexec = QueryExecutionFactory.create(jenaQuery, model);
        final var results = qexec.execSelect();

        results.forEachRemaining(solution -> {

            markers.forEach(marker ->{
                if(solution.contains(marker)) {
                    resultSet.get(marker).add(solution.get(marker).asResource().getURI());
                }
            });

            final var model = this.sparqlEndpointService.getModelFromResults(query, solution, schema);
            unionModel.add(model);
        });

        return new SPARQLExecutionResult(resultSet, unionModel);
    }

}