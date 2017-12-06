package org.hypergraphql.config.schema;

import org.hypergraphql.datafetching.services.Service;

import java.util.Map;

public class TypeConfig implements SchemElementConfig {

    private String id;

    private Map<String, FieldOfTypeConfig> fields;

    public TypeConfig(String id, Map<String, FieldOfTypeConfig> fields) {

        if (id!=null) this.id = id;
        this.fields=fields;

    }

    public String id() { return this.id; }

    public FieldOfTypeConfig getField(String name) { return this.fields.get(name); }

    @Override
    public Service service() {
        return null;
    }
}
