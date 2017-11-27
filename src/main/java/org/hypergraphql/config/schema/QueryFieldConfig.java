package org.hypergraphql.config.schema;

import org.hypergraphql.config.Service;

public class QueryFieldConfig implements SchemElementConfig {

    private Service service;

    public QueryFieldConfig(Service service) {

        if (service!=null) this.service = service;

    }
    public Service service() { return this.service; }


    @Override
    public String id() {
        return null;
    }
}
