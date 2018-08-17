package org.hypergraphql.config.system;

import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLType;
import org.hypergraphql.config.schema.FieldConfig;
import org.hypergraphql.config.schema.FieldOfTypeConfig;
import org.hypergraphql.config.schema.TypeConfig;
import org.hypergraphql.datamodel.HGQLSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FetchParamsTest {

    @Test
    @DisplayName("Happy path for a Query type")
    void happy_path_query_config() {

        String uri = "abc123";

        HGQLSchema schema = mock(HGQLSchema.class);
        DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);

        Field field1 = mock(Field.class);

        List<Field> fields = Collections.singletonList(field1);
        when(environment.getFields()).thenReturn(fields);
        when(field1.getName()).thenReturn("field1");

        FieldConfig fieldConfig = mock(FieldConfig.class);
        Map<String, FieldConfig> schemaFields = Collections.singletonMap("field1", fieldConfig);
        when(schema.getFields()).thenReturn(schemaFields);

        when(fieldConfig.getId()).thenReturn(uri);

        GraphQLType parent = mock(GraphQLType.class);
        when(environment.getParentType()).thenReturn(parent);
        when(parent.getName()).thenReturn("Query");

        FetchParams fetchParams = new FetchParams(environment, schema);

        assertNotNull(fetchParams);
        assertEquals(uri, fetchParams.getPredicateURI());
    }

    @Test
    @DisplayName("Happy path for a non-Query type with no target")
    void happy_path_non_query_type_with_no_target() {

        String uri = "abc123";

        HGQLSchema schema = mock(HGQLSchema.class);
        DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);

        Field field1 = mock(Field.class);

        List<Field> fields = Collections.singletonList(field1);
        when(environment.getFields()).thenReturn(fields);
        when(field1.getName()).thenReturn("field1");

        FieldConfig fieldConfig = mock(FieldConfig.class);
        Map<String, FieldConfig> schemaFields = Collections.singletonMap("field1", fieldConfig);
        when(schema.getFields()).thenReturn(schemaFields);

        when(fieldConfig.getId()).thenReturn(uri);

        GraphQLType parent = mock(GraphQLType.class);
        when(environment.getParentType()).thenReturn(parent);
        when(parent.getName()).thenReturn("non-Query");

        TypeConfig typeConfig = mock(TypeConfig.class);
        Map<String, TypeConfig> types = Collections.singletonMap("non-Query", typeConfig);
        when(schema.getTypes()).thenReturn(types);
        FieldOfTypeConfig fieldOfTypeConfig = mock(FieldOfTypeConfig.class);
        when(typeConfig.getField("field1")).thenReturn(fieldOfTypeConfig);
        when(fieldOfTypeConfig.getTargetName()).thenReturn("non-Query");

        when(typeConfig.getId()).thenReturn("targetUri");

        FetchParams fetchParams = new FetchParams(environment, schema);

        assertNotNull(fetchParams);
        assertEquals(uri, fetchParams.getPredicateURI());
        assertEquals("targetUri", fetchParams.getTargetURI());
    }
}