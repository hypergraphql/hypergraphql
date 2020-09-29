package org.hypergraphql.config.schema;

import java.util.Map;

public class TypeConfig {

    private final String id;
    private final String name;
    private final Map<String, FieldOfTypeConfig> fields;

    public TypeConfig(
            final String name,
            final String id,
            final Map<String, FieldOfTypeConfig> fields) {

        this.name = name;
        this.id = id;
        this.fields = fields;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return this.id;
    }

    public FieldOfTypeConfig getField(final String name) {
        return this.fields.get(name);
    }

    public Map<String, FieldOfTypeConfig> getFields() {
        return fields;
    }
}
