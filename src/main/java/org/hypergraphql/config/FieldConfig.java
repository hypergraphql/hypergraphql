package org.hypergraphql.config;

public class FieldConfig {
    private String id;
    private ServiceConfig service;

    public FieldConfig(String id, ServiceConfig service) {

        if (id!=null) this.id = id;
        if (service!=null) this.service = service;

    }

    public String id() { return this.id; }
    public ServiceConfig service() { return this.service; }

}
