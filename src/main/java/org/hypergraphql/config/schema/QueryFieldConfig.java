package org.hypergraphql.config.schema;

import org.hypergraphql.datafetching.services.Service;

public class QueryFieldConfig {

    private final Service service;
    private final String type;

    public QueryFieldConfig(final Service service, final String type) {

        this.service = service;
        this.type = type;

    }

    public Service service() {
        return this.service;
    }

    public String type() {
        return this.type;
    }
}
