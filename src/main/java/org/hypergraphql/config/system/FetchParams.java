package org.hypergraphql.config.system;

import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import org.apache.jena.rdf.model.Resource;
import org.hypergraphql.datamodel.HGQLSchema;
import org.hypergraphql.datamodel.HGQLSchemaWiring;
import org.hypergraphql.datamodel.ModelContainer;

public class FetchParams {

    private Resource subjectResource;
    private String predicateURI;
    private ModelContainer client;
    private String targetURI;



    public FetchParams(DataFetchingEnvironment environment, HGQLSchema hgqlschema) {

        subjectResource = environment.getSource();
        String predicate = ((Field) environment.getFields().toArray()[0]).getName();
        predicateURI = hgqlschema.getFields().get(predicate).getId();
        client = environment.getContext();
        if (!environment.getParentType().getName().equals("Query")) {
            String targetName = hgqlschema.getTypes().get(environment.getParentType().getName()).getField(predicate).getTargetName();
            if (hgqlschema.getTypes().containsKey(targetName) && hgqlschema.getTypes().get(targetName).getId()!=null) {
                targetURI=hgqlschema.getTypes().get(targetName).getId();
            }
        }
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
    public String getTargetURI() {return targetURI; }

}
