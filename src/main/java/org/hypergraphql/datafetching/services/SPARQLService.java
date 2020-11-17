package org.hypergraphql.datafetching.services;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collection;
import lombok.Getter;
import lombok.Setter;
import org.hypergraphql.config.system.ServiceConfig;
import org.hypergraphql.datafetching.SPARQLEndpointExecution;
import org.hypergraphql.datamodel.HGQLSchema;

@Getter
@Setter
public abstract class SPARQLService extends Service {

    private String graph;

    public void setParameters(final ServiceConfig serviceConfig) {

        setId(serviceConfig.getId());
        if (serviceConfig.getGraph() == null) {
            this.graph = "";
        } else {
            this.graph = serviceConfig.getGraph();
        }
    }

    protected abstract SPARQLEndpointExecution buildExecutor(
            JsonNode query,
            Collection<String> inputSubset,
            Collection<String> markers,
            HGQLSchema schema,
            String rootType
    );
}
