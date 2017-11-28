package org.hypergraphql.datamodel;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public class StoredModel {
    private static Model instance = null;
    protected StoredModel() {
        // Exists only to defeat instantiation.
    }
    public static Model getInstance() {
        if(instance == null) {
            instance = ModelFactory.createDefaultModel();
        }
        return instance;
    }
}