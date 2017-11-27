package org.hypergraphql.config;

import graphql.language.*;
import org.hypergraphql.ExecutionForest;
import org.hypergraphql.ExecutionTreeNode;

import java.util.List;

public class ExecutionForestFactory {

    HGQLConfig config;

    public ExecutionForestFactory(HGQLConfig config) {
        this.config = config;
    }


    public ExecutionForest getExecutionForest(Document queryDocument ) {

        ExecutionForest forest = new ExecutionForest();

        OperationDefinition opDef = (OperationDefinition) queryDocument.getDefinitions().get(0);
        SelectionSet queryFields = opDef.getSelectionSet();
        List<Selection> selections = queryFields.getSelections();

        for (Selection child : selections) {

            if (child.getClass().getSimpleName().equals("Field")) {

                forest.add(new ExecutionTreeNode(config, (Field) child, "x"));

            }

        }

        return forest;
    }
}
