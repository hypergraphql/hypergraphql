package org.hypergraphql.datafetching;

import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutionForestTest {

    @Test
    @DisplayName("Happy path for generating model")
    void should_generate_model() {

        final ExecutionForest executionForest = new ExecutionForest();
        final Model model = executionForest.generateModel();

        assertNotNull(model);
        assertTrue(model.isEmpty());
    }
}