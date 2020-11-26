package org.hypergraphql.datamodel;

import graphql.schema.idl.TypeDefinitionRegistry;
import java.util.ArrayList;
import lombok.val;
import org.hypergraphql.exception.HGQLConfigurationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class HGQLSchemaWiringTest {

    @Test
    @DisplayName("Constructor exception with nulls")
    void should_throw_exception_on_construction_from_nulls() {

        final Executable executable = () -> new HGQLSchemaWiring(null, null, null);
        val exception = assertThrows(HGQLConfigurationException.class, executable);
        assertEquals("Registry cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Constructor exception with nulls for first parameter")
    void should_throw_exception_on_construction_from_first_null() {

        final Executable executable = () -> new HGQLSchemaWiring(null, "local", new ArrayList<>());
        val exception = assertThrows(HGQLConfigurationException.class, executable);
        assertEquals("Registry cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Constructor exception with nulls for first 2 parameters")
    void should_throw_exception_on_construction_from_first_2_null() {

        final Executable executable = () -> new HGQLSchemaWiring(null, null, new ArrayList<>());
        val exception = assertThrows(HGQLConfigurationException.class, executable);
        assertEquals("Registry cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Constructor exception with nulls for last 2 parameters")
    void should_throw_exception_on_construction_from_last_2_null() {

        val registry = mock(TypeDefinitionRegistry.class);
        final Executable executable = () -> new HGQLSchemaWiring(registry, null, null);
        val exception = assertThrows(HGQLConfigurationException.class, executable);
        assertEquals("Schema name cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Constructor exception with null for last parameter")
    void should_throw_exception_on_construction_from_last_null() {

        val registry = mock(TypeDefinitionRegistry.class);
        final Executable executable = () -> new HGQLSchemaWiring(registry, "local", null);
        val exception = assertThrows(HGQLConfigurationException.class, executable);
        assertEquals("Service configurations cannot be null", exception.getMessage());
    }
}
