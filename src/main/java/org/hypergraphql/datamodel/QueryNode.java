package org.hypergraphql.datamodel;

import org.apache.jena.rdf.model.Property;

public class QueryNode {

    private final Property node;
    private final String marker;

    public QueryNode(final Property node, final String marker) {
        this.node = node;
        this.marker = marker;
    }

    public Property getNode() {
        return node;
    }

    public String getMarker() {
        return marker;
    }

    @Override
    public String toString() {
        return "QueryNode{"
                + "node=" + node
                + ", marker='" + marker + '\''
                + '}';
    }
}
