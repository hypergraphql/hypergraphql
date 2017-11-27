package org.hypergraphql.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import graphql.language.*;
import org.hypergraphql.ExecutionForrest;
import org.hypergraphql.TreeExecutionNode;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TreeExecutionFactory {

    HGQLConfig config;

    public TreeExecutionFactory(HGQLConfig config) {
        this.config = config;
    }


    public ExecutionForrest getExecutionTree(Document queryDocument ) {

        ExecutionForrest forest = new ExecutionForrest();

        OperationDefinition opDef = (OperationDefinition) queryDocument.getDefinitions().get(0);
        SelectionSet queryFields = opDef.getSelectionSet();
        List<Selection> fields = queryFields.getSelections();

        for (Selection child : fields) {

                if (child.getClass().getSimpleName().equals("Field")) {

                    forest.add(new TreeExecutionNode(config, (Field) child));
                }

        }





        Map<String, Object> conversionResult = getSelectionJson(opDef.getSelectionSet());

        JsonNode topQueries = (ArrayNode) conversionResult.get("query");
        Map<String, String> context = (Map<String, String>) conversionResult.get("context");
        result.put("query", topQueries);
        result.put("context", context);



        return null;
    }
}
