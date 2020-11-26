package org.hypergraphql.datafetching.services;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.jena.query.ARQ;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.hypergraphql.config.system.ServiceConfig;
import org.hypergraphql.datafetching.LocalSPARQLExecution;
import org.hypergraphql.datafetching.SPARQLEndpointExecution;
import org.hypergraphql.datamodel.HGQLSchema;
import org.hypergraphql.exception.HGQLConfigurationException;
import org.hypergraphql.util.LangUtils;

@Getter
@Slf4j
public final class LocalModelSPARQLService extends SPARQLEndpointService {

    private Model model;

    @Override
    public void setParameters(final ServiceConfig serviceConfig) {
        super.setParameters(serviceConfig);

        ARQ.init();

        setId(serviceConfig.getId());

        log.debug("Current path: " + new File(".").getAbsolutePath());

        val cwd = new File(".");
        try (var fis = new FileInputStream(new File(cwd, serviceConfig.getFilepath()));
            var in = new BufferedInputStream(fis)) {
            this.model = ModelFactory.createDefaultModel();
            val lang = LangUtils.forName(serviceConfig.getFiletype());
            RDFDataMgr.read(model, in, lang);
        } catch (FileNotFoundException e) {
            throw new HGQLConfigurationException("Unable to locate local RDF file", e);
        } catch (IOException e) {
            throw new HGQLConfigurationException("Nonspecific IO exception", e);
        }
    }

    @Override
    protected SPARQLEndpointExecution buildExecutor(
            final JsonNode query,
            final Collection<String> inputSubset,
            final Collection<String> markers,
            final HGQLSchema schema,
            final String rootType
    ) {
        return new LocalSPARQLExecution(query, inputSubset, markers, this, schema, getModel(), rootType);
    }
}
