package org.hypergraphql.config.schema;

import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class TypeConfig {

    private final String typeName;
    private final String id;
    private final Map<String, FieldOfTypeConfig> fields;

    public String getName() {
        return typeName;
    }

    public FieldOfTypeConfig getField(final String name) {
        return this.fields.get(name);
    }
}
