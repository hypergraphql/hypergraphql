package org.hypergraphql.datafetching;

import graphql.language.*;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.datafetching.services.SPARQLEndpointService;
import org.hypergraphql.datafetching.services.SPARQLService;
import org.hypergraphql.datamodel.HGQLSchema;
import org.hypergraphql.query.converters.SPARQLServiceConverter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        System.out.println(forest.toString());
        return forest;
    }
}
