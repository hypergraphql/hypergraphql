package org.hypergraphql.config.schema;

import org.hypergraphql.datafetching.services.Service;

import java.util.Map;

public class TypeConfig  {

    public String getName() {
        return name;
    }

    public String getId() {
        return this.id;
    }

    public FieldOfTypeConfig getField(String name) {
        return this.fields.get(name);
    }

    private String id;

    private String name;

    public Map<String, FieldOfTypeConfig> getFields() {
        return fields;
    }

    private Map<String, FieldOfTypeConfig> fields;

    public TypeConfig(String name, String id, Map<String, FieldOfTypeConfig> fields) {

        this.name=name;
        this.id = id;
        this.fields=fields;

    }

}
