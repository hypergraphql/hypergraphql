package org.hypergraphql.datafetching;

import graphql.language.*;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.datafetching.services.SPARQLEndpointService;
import org.hypergraphql.datafetching.services.SPARQLService;
import org.hypergraphql.query.converters.SPARQLServiceConverter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExecutionForestFactory {

    HGQLConfig config;

    public ExecutionForestFactory() {
        this.config = HGQLConfig.getInstance();
    }


    public ExecutionForest getExecutionForest(Document queryDocument ) {

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

                forest.getForest().add(new ExecutionTreeNode(field, nodeId));

            }
        }

        // System.out.println(forest.toString(0));

        return forest;
    }
}
