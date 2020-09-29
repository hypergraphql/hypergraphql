package org.hypergraphql.datafetching.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.jena.query.ARQ;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.hypergraphql.config.system.ServiceConfig;
import org.hypergraphql.datafetching.LocalSPARQLExecution;
import org.hypergraphql.datafetching.SPARQLExecutionResult;
import org.hypergraphql.datafetching.TreeExecutionResult;
import org.hypergraphql.datamodel.HGQLSchema;
import org.hypergraphql.exception.HGQLConfigurationException;
import org.hypergraphql.util.LangUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class LocalModelSPARQLService extends SPARQLEndpointService{

    private final static Logger LOGGER = LoggerFactory.getLogger(LocalModelSPARQLService.class);

    protected Model model;

    @Override
    public TreeExecutionResult executeQuery(
            final JsonNode query,
            final Set<String> input,
            final Set<String> markers,
            final String rootType,
            final HGQLSchema schema) {

        final Map<String, Set<String>> resultSet = new HashMap<>(); // TODO - dupe
        final var unionModel = ModelFactory.createDefaultModel();
        final Set<Future<SPARQLExecutionResult>> futureSPARQLResults = new HashSet<>();

        final List<String> inputList = getStrings(query, input, markers, rootType, schema, resultSet);

        do {

            final Set<String> inputSubset = new HashSet<>(); // TODO - dupe
            int i = 0;
            while (i < VALUES_SIZE_LIMIT && !inputList.isEmpty()) { // TODO - inner loop
                inputSubset.add(inputList.get(0));
                inputList.remove(0);
                i++;
            }
            final var executor = Executors.newFixedThreadPool(50);
            final var execution = new LocalSPARQLExecution(query,inputSubset,markers,this, schema , this.model, rootType);
            futureSPARQLResults.add(executor.submit(execution));

        } while (inputList.size() > VALUES_SIZE_LIMIT);

        iterateFutureResults(futureSPARQLResults, unionModel, resultSet);

        final var treeExecutionResult = new TreeExecutionResult();
        treeExecutionResult.setResultSet(resultSet);
        treeExecutionResult.setModel(unionModel);

        return treeExecutionResult;
    }

    @Override
    public void setParameters(final ServiceConfig serviceConfig) {
        super.setParameters(serviceConfig);

        ARQ.init();

        this.id = serviceConfig.getId();

        LOGGER.debug("Current path: " + new File(".").getAbsolutePath());

        final var cwd = new File(".");
        try(final var fis = new FileInputStream(new File(cwd, serviceConfig.getFilepath()));
            final var in = new BufferedInputStream(fis)) {
            this.model = ModelFactory.createDefaultModel();
            final var lang = LangUtils.forName(serviceConfig.getFiletype());
            RDFDataMgr.read(model, in, lang);
        } catch (FileNotFoundException e) {
            throw new HGQLConfigurationException("Unable to locate local RDF file", e);
        } catch (IOException e) {
            throw new HGQLConfigurationException("Nonspecific IO exception", e);
        }
    }
}