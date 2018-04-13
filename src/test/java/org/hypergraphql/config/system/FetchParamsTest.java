package org.hypergraphql.config.system;

import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLType;
import org.hypergraphql.config.schema.FieldConfig;
import org.hypergraphql.config.schema.FieldOfTypeConfig;
import org.hypergraphql.config.schema.TypeConfig;
import org.hypergraphql.datamodel.HGQLSchema;
import org.hypergraphql.exception.HGQLConfigurationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    @DisplayName("Environment must have a parent type")
    void environment_must_have_parent_type() {

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

        Executable executable = () -> new FetchParams(environment, schema);
        Throwable throwable = assertThrows(HGQLConfigurationException.class, executable);
        assertEquals("Parent type cannot be null", throwable.getMessage());
    }

    @Test
    @DisplayName("Environment parent type must have a name")
    void environment_must_have_parent_type_name() {

        HGQLSchema schema = mock(HGQLSchema.class);
        DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);

        Field field1 = mock(Field.class);

        List<Field> fields = Collections.singletonList(field1);
        when(environment.getFields()).thenReturn(fields);
        when(field1.getName()).thenReturn("field1");

        FieldConfig fieldConfig = mock(FieldConfig.class);
        Map<String, FieldConfig> schemaFields = Collections.singletonMap("field1", fieldConfig);
        when(schema.getFields()).thenReturn(schemaFields);

        GraphQLType parentType = mock(GraphQLType.class);
        when(environment.getParentType()).thenReturn(parentType);

        Executable executable = () -> new FetchParams(environment, schema);
        Throwable throwable = assertThrows(HGQLConfigurationException.class, executable);
        assertEquals("'name' is a required field for HGQL types", throwable.getMessage());
    }

    @Test
    @DisplayName("Environment must have non-null fields value")
    void environment_must_have_non_null_fields() {

        Executable executable = () -> new FetchParams(
                mock(DataFetchingEnvironment.class),
                mock(HGQLSchema.class)
        );
        Throwable throwable = assertThrows(HGQLConfigurationException.class, executable);
        assertEquals("Environment must have at least one field", throwable.getMessage());
    }

    @Test
    @DisplayName("Environment must have more than zero fields")
    void environment_must_have_more_than_zero_fields() {

        DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);
        when(environment.getFields()).thenReturn(Collections.emptyList());

        Executable executable = () -> new FetchParams(
                environment,
                mock(HGQLSchema.class)
        );
        Throwable throwable = assertThrows(HGQLConfigurationException.class, executable);
        assertEquals("Environment must have at least one field", throwable.getMessage());
    }

    @Test
    @DisplayName("schema must have some config")
    void schema_must_have_config() {

        DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);
        when(environment.getFields()).thenReturn(Collections.singletonList(mock(Field.class)));

        Executable executable = () -> new FetchParams(
                environment,
                mock(HGQLSchema.class)
        );
        Throwable throwable = assertThrows(HGQLConfigurationException.class, executable);
        assertEquals("Schema has no fields", throwable.getMessage());
    }

    @Test
    @DisplayName("schema must not have empty config")
    void schema_must_have_config_2() {

        DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);
        when(environment.getFields()).thenReturn(Collections.singletonList(mock(Field.class)));

        HGQLSchema schema = mock(HGQLSchema.class);
        when(schema.getFields()).thenReturn(Collections.emptyMap());

        Executable executable = () -> new FetchParams(
                environment,
                schema
        );
        Throwable throwable = assertThrows(HGQLConfigurationException.class, executable);
        assertEquals("Schema has no fields", throwable.getMessage());
    }

    @Test
    @DisplayName("schema must have config for predicate")
    void schema_must_have_config_for_predicate() {

        DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);
        Field field = mock(Field.class);
        when(environment.getFields()).thenReturn(Collections.singletonList(field));
        when(field.getName()).thenReturn("field1");

        HGQLSchema schema = mock(HGQLSchema.class);
        FieldConfig fieldConfig = mock(FieldConfig.class);
        when(schema.getFields()).thenReturn(Collections.singletonMap("field", fieldConfig));

        Executable executable = () -> new FetchParams(
                environment,
                schema
        );
        Throwable throwable = assertThrows(HGQLConfigurationException.class, executable);
        assertEquals("No field configuration for 'field1'", throwable.getMessage());
    }
}