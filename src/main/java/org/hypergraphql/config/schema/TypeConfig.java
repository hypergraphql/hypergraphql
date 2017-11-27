package org.hypergraphql.config.schema;

import org.hypergraphql.config.Service;

public class TypeConfig implements SchemElementConfig {

    private String id;

    public TypeConfig(String id) {

        if (id!=null) this.id = id;

    }

    public String id() { return this.id; }

    @Override
    public Service service() {
        return null;
    }
}
