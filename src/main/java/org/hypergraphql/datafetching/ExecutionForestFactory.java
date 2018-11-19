package org.hypergraphql.datafetching;

import graphql.language.Definition;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.OperationDefinition;
import graphql.language.SelectionSet;
import org.hypergraphql.datamodel.HGQLSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecutionForestFactory {

    private final static Logger LOGGER = LoggerFactory.getLogger(ExecutionForestFactory.class);

    public ExecutionForest getExecutionForest(Document queryDocument , HGQLSchema schema) {

        ExecutionForest forest = new ExecutionForest();

        SelectionSet queryFields = selectionSet(queryDocument);

        final AtomicInteger counter = new AtomicInteger(0);
        queryFields.getSelections().forEach(child -> { // query fields - why no args?

            if (child.getClass().getSimpleName().equals("Field")) {

                String nodeId = "x_" + counter.incrementAndGet();
                forest.getForest().add(new ExecutionTreeNode((Field) child, nodeId , schema));

            }
        });
        return forest;
    }

    private SelectionSet selectionSet(final Document queryDocument) {

        final Definition definition = queryDocument.getDefinitions().get(0);

        if(definition.getClass().isAssignableFrom(FragmentDefinition.class)) {

            return getFragmentSelectionSet(queryDocument);

        } else if(definition.getClass().isAssignableFrom(OperationDefinition.class)) {
            final OperationDefinition operationDefinition = (OperationDefinition)definition;
            return operationDefinition.getSelectionSet();
        }
        throw new IllegalArgumentException(queryDocument.getClass().getName() + " is not supported");
    }

    private SelectionSet getFragmentSelectionSet(final Document queryDocument) {

        // NPE
        final FragmentDefinition fragmentDefinition = (FragmentDefinition)queryDocument.getDefinitions().get(0);
        final SelectionSet originalSelectionSet = fragmentDefinition.getSelectionSet();

        final Optional<Definition> optionalDefinition = queryDocument.getDefinitions()
                .stream()
                .filter(def -> def.getClass().isAssignableFrom(OperationDefinition.class))
                .findFirst();

        final OperationDefinition operationDefinition;
        if(optionalDefinition.isPresent()) {
            operationDefinition = (OperationDefinition)optionalDefinition.get();
        } else {
            // bail
            throw new IllegalArgumentException("No OperationDefinition is available within the query");
        }

        // NPE?
        final Field operationSelection = (Field)operationDefinition.getSelectionSet().getSelections().get(0);

        final String typeFieldName = operationSelection.getName();

        final Field newSelection = Field.newField()
                .name(typeFieldName)
                .arguments(operationSelection.getArguments())
                .selectionSet(originalSelectionSet)
                .build();

        return new SelectionSet(Collections.singletonList(newSelection));
    }
}
