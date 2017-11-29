package org.hypergraphql.config.schema;

import org.hypergraphql.datafetching.services.Service;

public interface SchemElementConfig {


    public String id();
    public Service service();

}
