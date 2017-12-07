package org.hypergraphql.config.schema;

import org.hypergraphql.datafetching.services.Service;

public class FieldConfig {

    private String id;
    private Service service;

    public FieldConfig(String id, Service service) {

        this.id = id;
        this.service = service;

    }

    public String id() { return this.id; }
    public Service service() { return this.service; }

}
