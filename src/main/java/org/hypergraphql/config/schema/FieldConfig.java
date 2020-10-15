package org.hypergraphql.config.schema;

public class FieldConfig {

    private final String id;

    public FieldConfig(String id) {

        this.id = id;
    }

    public String getId() {
        return this.id;
    }
}
