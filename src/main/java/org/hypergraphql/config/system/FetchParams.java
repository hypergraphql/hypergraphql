package org.hypergraphql.config.system;

import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLType;
import org.apache.jena.rdf.model.Resource;
import org.hypergraphql.config.schema.FieldConfig;
import org.hypergraphql.config.schema.TypeConfig;
import org.hypergraphql.datamodel.HGQLSchema;
import org.hypergraphql.datamodel.ModelContainer;
import org.hypergraphql.exception.HGQLConfigurationException;

import java.util.List;
import java.util.Map;

public class FetchParams {

    private Resource subjectResource;
    private String predicateURI;
    private ModelContainer client;
    private String targetURI;

    public FetchParams(DataFetchingEnvironment environment, HGQLSchema hgqlSchema)
            throws HGQLConfigurationException {

        subjectResource = environment.getSource();
        String predicate = extractPredicate(environment);
        predicateURI = extractPredicateUri(hgqlSchema, predicate);
        client = environment.getContext();
        targetURI = extractTargetURI(environment, hgqlSchema, predicate);
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

    private String extractPredicate(DataFetchingEnvironment environment) {

        final List<Field> fields = environment.getFields();
        if(fields == null || fields.isEmpty()) {
            throw new HGQLConfigurationException("Environment must have at least one field");
        }
        return fields.get(0).getName();
    }

    private String extractPredicateUri(final HGQLSchema schema, final String predicate) {

        final Map<String, FieldConfig> fields = schema.getFields();

        if(fields == null || fields.isEmpty()) {
            throw new HGQLConfigurationException("Schema has no fields");
        }

        final FieldConfig fieldConfig = fields.get(predicate);

        if(fieldConfig == null) {
            throw new HGQLConfigurationException("No field configuration for '" + predicate + "'");
        }

        return fieldConfig.getId();
    }

    private String extractTargetURI(final DataFetchingEnvironment environment, final HGQLSchema schema, final String predicate) {

        final GraphQLType parentType = environment.getParentType();
        if(parentType == null) {
            throw new HGQLConfigurationException("Parent type cannot be null");
        }

        final String parentTypeName = parentType.getName();

        if (parentTypeName == null) {
            throw new HGQLConfigurationException("'name' is a required field for HGQL types");
        }

        if (!parentTypeName.equals("Query")) {

            final TypeConfig typeConfig = schema.getTypes().get(parentTypeName);

            if(typeConfig == null || typeConfig.getField(predicate) == null) {
                throw new HGQLConfigurationException("typeConfig must be valid and have a value for '" + predicate + "'");
            }

            String targetName = typeConfig.getField(predicate).getTargetName();
            if (schema.getTypes().containsKey(targetName) && schema.getTypes().get(targetName).getId() != null) {
                return schema.getTypes().get(targetName).getId();
            } else {
                throw new HGQLConfigurationException("schema must have a value for '" + targetName + "'");
            }
        }

        return null;
    }

}
