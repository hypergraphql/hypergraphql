package org.hypergraphql.datamodel;

import graphql.schema.idl.TypeDefinitionRegistry;
import org.hypergraphql.exception.HGQLConfigurationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class HGQLSchemaWiringTest {

    @Test
    @DisplayName("Constructor exception with nulls")
    void should_throw_exception_on_construction_from_nulls() {

        Executable executable = () -> new HGQLSchemaWiring(null, null, null);
        Throwable exception = assertThrows(HGQLConfigurationException.class, executable);
        assertEquals("Unable to perform schema wiring", exception.getMessage());
    }

    @Test
    @DisplayName("Constructor exception with nulls for first parameter")
    void should_throw_exception_on_construction_from_first_null() {

        Executable executable = () -> new HGQLSchemaWiring(null, "local", new ArrayList<>());
        Throwable exception = assertThrows(HGQLConfigurationException.class, executable);
        assertEquals("Unable to perform schema wiring", exception.getMessage());
    }

    @Test
    @DisplayName("Constructor exception with nulls for first 2 parameters")
    void should_throw_exception_on_construction_from_first_2_null() {

        Executable executable = () -> new HGQLSchemaWiring(null, null, new ArrayList<>());
        Throwable exception = assertThrows(HGQLConfigurationException.class, executable);
        assertEquals("Unable to perform schema wiring", exception.getMessage());
    }

    @Test
    @DisplayName("Constructor exception with nulls for last 2 parameters")
    void should_throw_exception_on_construction_from_last_2_null() {

        TypeDefinitionRegistry registry = mock(TypeDefinitionRegistry.class);
        Executable executable = () -> new HGQLSchemaWiring(registry, null, null);
        Throwable exception = assertThrows(HGQLConfigurationException.class, executable);
        assertEquals("Unable to perform schema wiring", exception.getMessage());
    }

    @Test
    @DisplayName("Constructor exception with null for last parameter")
    void should_throw_exception_on_construction_from_last_null() {

        TypeDefinitionRegistry registry = mock(TypeDefinitionRegistry.class);
        Executable executable = () -> new HGQLSchemaWiring(registry, "local", null);
        Throwable exception = assertThrows(HGQLConfigurationException.class, executable);
        assertEquals("Unable to perform schema wiring", exception.getMessage());
    }
}