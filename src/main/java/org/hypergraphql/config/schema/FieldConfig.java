package org.hypergraphql.config.schema;

import org.hypergraphql.datafetching.services.Service;

public class FieldConfig implements SchemElementConfig{

    private String id;
    private Service service;

    public FieldConfig(String id, Service service) {

        if (id!=null) this.id = id;
        if (service!=null) this.service = service;

    }

    public String id() { return this.id; }
    public Service service() { return this.service; }

}
