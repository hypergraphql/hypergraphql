package org.hypergraphql.config;

public class TypeConfig {

    private String id;

    public TypeConfig(String id) {

        if (id!=null) this.id = id;

    }

    public String id() { return this.id; }
}
