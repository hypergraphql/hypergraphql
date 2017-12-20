package org.hypergraphql.datafetching;

import graphql.language.*;
import org.hypergraphql.datamodel.HGQLSchema;

import java.util.List;

public class ExecutionForestFactory {

    public ExecutionForest getExecutionForest(Document queryDocument , HGQLSchema schema) {

        ExecutionForest forest = new ExecutionForest();

        OperationDefinition opDef = (OperationDefinition) queryDocument.getDefinitions().get(0);
        SelectionSet queryFields = opDef.getSelectionSet();
        List<Selection> selections = queryFields.getSelections();

        int i = 0;

        for (Selection child : selections) {

            if (child.getClass().getSimpleName().equals("Field")) {

                i++;

                String nodeId = "x_" + i;
                Field field = (Field) child;

                forest.getForest().add(new ExecutionTreeNode(field, nodeId , schema));

            }
        }
        return forest;
    }
}
