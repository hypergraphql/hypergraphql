package org.hypergraphql.datamodel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.jena.rdf.model.Property;

@RequiredArgsConstructor
@Getter
public class QueryNode {

    private final Property node;
    private final String marker;

    @Override
    public String toString() {
        return "QueryNode{"
                + "node=" + node
                + ", marker='" + marker + '\''
                + '}';
    }
}
