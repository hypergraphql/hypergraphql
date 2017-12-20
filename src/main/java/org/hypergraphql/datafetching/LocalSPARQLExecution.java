package org.hypergraphql.datafetching;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.jena.query.*;
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


    public LocalSPARQLExecution(JsonNode query, Set<String> inputSubset, Set<String> markers, SPARQLEndpointService sparqlEndpointService, HGQLSchema schema , Model localmodel, String rootType) {
        super(query, inputSubset, markers, sparqlEndpointService, schema, rootType);
        this.model = localmodel;
    }

    @Override
    public SPARQLExecutionResult call() throws Exception {
        Map<String, Set<String>> resultSet = new HashMap<>();
        for (String marker : markers) {
            resultSet.put(marker, new HashSet<>());
        }

        Model unionModel = ModelFactory.createDefaultModel();


        SPARQLServiceConverter converter = new SPARQLServiceConverter(schema);
        String sparqlQuery = converter.getSelectQuery(query, inputSubset, rootType);
        logger.info(sparqlQuery);
        Query jenaQuery = QueryFactory.create(sparqlQuery);

        QueryExecution qexec = QueryExecutionFactory.create(jenaQuery, model);
            ResultSet results = qexec.execSelect();


        while (results.hasNext()) {
            QuerySolution solution = results.next();

            for (String marker : markers) {
                if (solution.contains(marker)) resultSet.get(marker).add(solution.get(marker).asResource().getURI());

            }

            Model model = this.sparqlEndpointService.getModelFromResults(query, solution, schema);
            unionModel.add(model);


        }

        SPARQLExecutionResult sparqlExecutionResult = new SPARQLExecutionResult(resultSet, unionModel);
        logger.info(sparqlExecutionResult);

        return sparqlExecutionResult;
    }

}
