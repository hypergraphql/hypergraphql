package org.hypergraphql.datafetching;

import graphql.language.*;
import org.hypergraphql.datamodel.HGQLSchema;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecutionForestFactory {

    public ExecutionForest getExecutionForest(Document queryDocument , HGQLSchema schema) {

        ExecutionForest forest = new ExecutionForest();

        OperationDefinition opDef = (OperationDefinition) queryDocument.getDefinitions().get(0);
        SelectionSet queryFields = opDef.getSelectionSet();
        List<Selection> selections = queryFields.getSelections();

        final AtomicInteger counter = new AtomicInteger(0);
        selections.forEach(child -> {

            if (child.getClass().getSimpleName().equals("Field")) {

                String nodeId = "x_" + counter.incrementAndGet();
                Field field = (Field) child;

                forest.getForest().add(new ExecutionTreeNode(field, nodeId , schema));

            }
        });
        return forest;
    }
}
