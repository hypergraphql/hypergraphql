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
import org.hypergraphql.datamodel.HGQLSchema;

public class ExecutionForestFactory {

    public ExecutionForest getExecutionForest(final Document queryDocument, final HGQLSchema schema) {

        final var forest = new ExecutionForest();

        final var queryFields = selectionSet(queryDocument);

        final var counter = new AtomicInteger(0);
        queryFields.getSelections().forEach(child -> { // query fields - why no args?

            if (child.getClass().getSimpleName().equals("Field")) {

                final var nodeId = "x_" + counter.incrementAndGet();
                forest.getForest().add(new ExecutionTreeNode((Field) child, nodeId, schema));

            }
        });
        return forest;
    }

    private SelectionSet selectionSet(final Document queryDocument) {

        final var definition = queryDocument.getDefinitions().get(0);

        if (definition.getClass().isAssignableFrom(FragmentDefinition.class)) {

            return getFragmentSelectionSet(queryDocument);

        } else if (definition.getClass().isAssignableFrom(OperationDefinition.class)) {
            final var operationDefinition = (OperationDefinition) definition;
            return operationDefinition.getSelectionSet();
        }
        throw new IllegalArgumentException(queryDocument.getClass().getName() + " is not supported");
    }

    private SelectionSet getFragmentSelectionSet(final Document queryDocument) {

        // NPE
        final var fragmentDefinition = (FragmentDefinition) queryDocument.getDefinitions().get(0);
        final var originalSelectionSet = fragmentDefinition.getSelectionSet();

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
        final var operationSelection = (Field) operationDefinition.getSelectionSet().getSelections().get(0);

        final var typeFieldName = operationSelection.getName();

        final var newSelection = Field.newField()
                .name(typeFieldName)
                .arguments(operationSelection.getArguments())
                .selectionSet(originalSelectionSet)
                .build();

        return new SelectionSet(Collections.singletonList(newSelection));
    }
}
