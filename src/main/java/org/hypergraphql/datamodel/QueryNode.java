package org.hypergraphql.datamodel;

import org.apache.jena.rdf.model.Property;

public class QueryNode {

    private Property node;
    private String marker;

    public Property getNode() {
        return node;
    }

    public void setNode(Property node) {
        this.node = node;
    }

    public String getMarker() {
        return marker;
    }

    public void setMarker(String marker) {
        this.marker = marker;
    }

    public QueryNode(Property node, String marker) {
        this.node = node;

        this.marker = marker;
    }

    @Override
    public String toString() {
        return "QueryNode{" +
                "node=" + node +
                ", marker='" + marker + '\'' +
                '}';
    }
}
