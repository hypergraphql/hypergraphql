package org.hypergraphql.config;

public class QueryFieldConfig {

    private Service service;

    public QueryFieldConfig(Service service) {

        if (service!=null) this.service = service;

    }
    public Service service() { return this.service; }


}
