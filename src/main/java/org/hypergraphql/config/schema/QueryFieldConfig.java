package org.hypergraphql.config.schema;

import org.hypergraphql.datafetching.services.Service;

public class QueryFieldConfig {

    private Service service;
    private String type;

    public QueryFieldConfig(Service service, String type ) {

        if (service!=null) this.service = service;
        this.type = type;

    }
    public Service service() { return this.service; }
    public String type() { return this.type; }


}
