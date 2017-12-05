package org.hypergraphql.config.system;

import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.hypergraphql.datamodel.ModelContainer;

public class FetchParams {
    public Resource getSubjectResource() {
        return subjectResource;
    }

    public Property getProperty() {
        return property;
    }

    public ModelContainer getClient() {
        return client;
    }

    private Resource subjectResource;
    private Property property;
    private ModelContainer client;

    public void FetchParams(DataFetchingEnvironment environment) {
        subjectResource = environment.getSource();
        String predicate = ((Field) environment.getFields().toArray()[0]).getName();
        String predicateURI = config.fields().get(predicate).id();
        client = environment.getContext();
        property = client.getPropertyFromUri(predicateURI);
    }
}
