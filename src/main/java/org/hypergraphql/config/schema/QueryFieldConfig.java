package org.hypergraphql.config.schema;

import lombok.RequiredArgsConstructor;
import org.hypergraphql.datafetching.services.Service;

@RequiredArgsConstructor
public class QueryFieldConfig {

    private final Service service;
    private final String type;

    public Service service() {
        return this.service;
    }

    public String type() {
        return this.type;
    }
}
