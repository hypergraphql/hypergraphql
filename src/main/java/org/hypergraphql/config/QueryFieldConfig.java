package org.hypergraphql.config;

public class QueryFieldConfig {

    private ServiceConfig service;

    public QueryFieldConfig(ServiceConfig service) {

        if (service!=null) this.service = service;

    }
    public ServiceConfig service() { return this.service; }


}
