package org.hypergraphql.datafetching;

import graphql.language.Definition;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.OperationDefinition;
import graphql.language.SelectionSet;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.val;
import org.hypergraphql.datamodel.HGQLSchema;

public class ExecutionForestFactory {

    public ExecutionForest getExecutionForest(final Document queryDocument, final HGQLSchema schema) {

        val forest = new ExecutionForest();
        val queryFields = selectionSet(queryDocument);
        val counter = new AtomicInteger(0);
        queryFields.getSelections().forEach(child -> { // query fields - why no args?

            if (child.getClass().isAssignableFrom(Field.class)) {
                val nodeId = "x_" + counter.incrementAndGet();
                forest.getForest().add(new ExecutionTreeNode((Field) child, nodeId, schema));
            }
        });
        return forest;
    }

    private SelectionSet selectionSet(final Document queryDocument) {

        val definition = queryDocument.getDefinitions().get(0);
        if (definition.getClass().isAssignableFrom(FragmentDefinition.class)) {
            return getFragmentSelectionSet(queryDocument);
        } else if (definition.getClass().isAssignableFrom(OperationDefinition.class)) {
            val operationDefinition = (OperationDefinition) definition;
            return operationDefinition.getSelectionSet();
        }
        throw new IllegalArgumentException(queryDocument.getClass().getName() + " is not supported");
    }

    private SelectionSet getFragmentSelectionSet(final Document queryDocument) {

        // NPE
        val fragmentDefinition = (FragmentDefinition) queryDocument.getDefinitions().get(0);
        val originalSelectionSet = fragmentDefinition.getSelectionSet();

        final Optional<Definition> optionalDefinition = queryDocument.getDefinitions()
                .stream()
                .filter(def -> def.getClass().isAssignableFrom(OperationDefinition.class))
                .findFirst();

        final OperationDefinition operationDefinition;
        if (optionalDefinition.isPresent()) {
            operationDefinition = (OperationDefinition) optionalDefinition.get();
        } else {
            // bail
            throw new IllegalArgumentException("No OperationDefinition is available within the query");
        }

        // NPE?
        val operationSelection = (Field) operationDefinition.getSelectionSet().getSelections().get(0);
        val typeFieldName = operationSelection.getName();
        val newSelection = Field.newField()
                .name(typeFieldName)
                .arguments(operationSelection.getArguments())
                .selectionSet(originalSelectionSet)
                .build();

        return new SelectionSet(Collections.singletonList(newSelection));
    }
}
