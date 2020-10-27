package org.hypergraphql.config.system;

import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLTypeUtil;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.apache.jena.rdf.model.Resource;
import org.hypergraphql.config.schema.FieldConfig;
import org.hypergraphql.datamodel.HGQLSchema;
import org.hypergraphql.datamodel.ModelContainer;
import org.hypergraphql.exception.HGQLConfigurationException;

@Getter
public class FetchParams {

    private final Resource subjectResource;
    private final String predicateURI;
    private final ModelContainer client;
    private final String targetURI;

    public FetchParams(final DataFetchingEnvironment environment, final HGQLSchema hgqlSchema)
            throws HGQLConfigurationException {

        final var predicate = extractPredicate(environment);
        predicateURI = extractPredicateUri(hgqlSchema, predicate);
        targetURI = extractTargetURI(environment, hgqlSchema, predicate);
        subjectResource = environment.getSource();
        client = environment.getContext();
    }

    private String extractPredicate(DataFetchingEnvironment environment) {

        final List<Field> fields = environment.getFields();
        if (fields == null || fields.isEmpty()) {
            return null;
        }
        return fields.get(0).getName();
    }

    private String extractPredicateUri(final HGQLSchema schema, final String predicate) {

        final Map<String, FieldConfig> fields = schema.getFields();

        if (fields == null || fields.isEmpty()) { // TODO :: Does this cause an issue?
            throw new HGQLConfigurationException("Schema has no fields");
        }

        final var fieldConfig = fields.get(predicate);
        if (fieldConfig != null) {
            return fieldConfig.getId();
        }
        return null;
    }

    // GraphQLNamedSchemaElement
    private String extractTargetURI(final DataFetchingEnvironment environment, final HGQLSchema schema, final String predicate) {

        final var parentTypeName = GraphQLTypeUtil.simplePrint(environment.getParentType());
//        final var parentTypeName = environment.getParentType().getName();
        if (!"Query".equals(parentTypeName)) {
            final var targetName =
                    schema.getTypes().get(parentTypeName).getField(predicate).getTargetName();

            if (schema.getTypes().containsKey(targetName)
                    && schema.getTypes().get(targetName).getId() != null) {
                return schema.getTypes().get(targetName).getId();
            }
        }
        return null;
    }

}
