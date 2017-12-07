package org.hypergraphql.config.system;

import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import org.apache.jena.rdf.model.Resource;
import org.hypergraphql.datamodel.HGQLSchemaWiring;
import org.hypergraphql.datamodel.ModelContainer;

public class FetchParams {

    private Resource subjectResource;
    private String predicateURI;
    private ModelContainer client;

    private HGQLSchemaWiring config = HGQLSchemaWiring.getInstance();

    public FetchParams(DataFetchingEnvironment environment) {

        subjectResource = environment.getSource();
        String predicate = ((Field) environment.getFields().toArray()[0]).getName();
        predicateURI = config.getFields().get(predicate).getId();
        client = environment.getContext();
    }

    public Resource getSubjectResource() {
        return subjectResource;
    }
    public String getPredicateURI() {
        return predicateURI;
    }
    public ModelContainer getClient() {
        return client;
    }

}
