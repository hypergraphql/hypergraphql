package org.hypergraphql.config.system;

import graphql.execution.MergedField;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLNamedType;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.val;
import org.hypergraphql.config.schema.FieldConfig;
import org.hypergraphql.config.schema.FieldOfTypeConfig;
import org.hypergraphql.config.schema.TypeConfig;
import org.hypergraphql.datamodel.HGQLSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FetchParamsTest {

    @Test
    @DisplayName("Happy path for a Query type")
    void happy_path_query_config() {

        val uri = "abc123";

        val schema = mock(HGQLSchema.class);
        val environment = mock(DataFetchingEnvironment.class);

        val field1 = mock(Field.class);

        final List<Field> fields = Collections.singletonList(field1);
        val mergedField = mock(MergedField.class);
        when(environment.getMergedField()).thenReturn(mergedField);
        when(environment.getMergedField().getFields()).thenReturn(fields);
        when(field1.getName()).thenReturn("field1");

        val fieldConfig = mock(FieldConfig.class);
        final Map<String, FieldConfig> schemaFields = Collections.singletonMap("field1", fieldConfig);
        when(schema.getFields()).thenReturn(schemaFields);

        when(fieldConfig.getId()).thenReturn(uri);

        val parent = mock(GraphQLNamedType.class);
        when(environment.getParentType()).thenReturn(parent);
        when(parent.getName()).thenReturn("Query");

        val fetchParams = new FetchParams(environment, schema);

        assertNotNull(fetchParams);
        assertEquals(uri, fetchParams.getPredicateURI());
    }

    @Test
    @DisplayName("Happy path for a non-Query type with no target")
    void happy_path_non_query_type_with_no_target() {

        val uri = "abc123";

        val schema = mock(HGQLSchema.class);
        val environment = mock(DataFetchingEnvironment.class);

        val field1 = mock(Field.class);

        final List<Field> fields = Collections.singletonList(field1);
        val mergedField = mock(MergedField.class);
        when(environment.getMergedField()).thenReturn(mergedField);
        when(environment.getMergedField().getFields()).thenReturn(fields);
        when(field1.getName()).thenReturn("field1");

        val fieldConfig = mock(FieldConfig.class);
        final Map<String, FieldConfig> schemaFields = Collections.singletonMap("field1", fieldConfig);
        when(schema.getFields()).thenReturn(schemaFields);

        when(fieldConfig.getId()).thenReturn(uri);

        val parent = mock(GraphQLNamedType.class);
        when(environment.getParentType()).thenReturn(parent);
        when(parent.getName()).thenReturn("non-Query");

        val typeConfig = mock(TypeConfig.class);
        final Map<String, TypeConfig> types = Collections.singletonMap("non-Query", typeConfig);
        when(schema.getTypes()).thenReturn(types);
        val fieldOfTypeConfig = mock(FieldOfTypeConfig.class);
        when(typeConfig.getField("field1")).thenReturn(fieldOfTypeConfig);
        when(fieldOfTypeConfig.getTargetName()).thenReturn("non-Query");

        when(typeConfig.getId()).thenReturn("targetUri");

        val fetchParams = new FetchParams(environment, schema);

        assertNotNull(fetchParams);
        assertEquals(uri, fetchParams.getPredicateURI());
        assertEquals("targetUri", fetchParams.getTargetURI());
    }
}
