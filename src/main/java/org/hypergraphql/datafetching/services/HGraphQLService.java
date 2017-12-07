package org.hypergraphql.datafetching.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.org.apache.xpath.internal.operations.Mod;
import org.apache.commons.lang.UnhandledException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.*;
import org.hypergraphql.config.schema.FieldConfig;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.datafetching.TreeExecutionResult;
import org.hypergraphql.datamodel.QueryNode;
import org.hypergraphql.query.converters.HGraphQLConverter;

import javax.jws.WebParam;
import java.util.*;

public class HGraphQLService extends Service {
    private String url;
    private String user;
    private String password;



    @Override
    public TreeExecutionResult executeQuery(JsonNode query, Set<String> input,  Set<String> markers) {

        Model model ;
        Map <String, Set<String>> resultSet ;
        JsonNode graphQlQuery = new HGraphQLConverter().convertToHGraphQL(query,input);
        model = getModelFromRemote(graphQlQuery);

        resultSet = getResultset(model,query,input,markers);

        TreeExecutionResult treeExecutionResult = new TreeExecutionResult();
        treeExecutionResult.setResultSet(resultSet);
        treeExecutionResult.setModel(model);

        return treeExecutionResult;
    }

    private Map<String,Set<String>> getResultset(Model model, JsonNode query, Set<String> input, Set<String> markers) {


        Map<String,Set<String>> resultset = new HashMap<>();

        Set<LinkedList<QueryNode>> paths = getQueryPaths(query);

        for (LinkedList<QueryNode> path : paths ) {

            if (hasMarkerLeaf(path,markers)) {
                Set<String> identifiers = findIdentifiers(model,input,path);
                String marker = getLeafMarker(path);
                resultset.put(marker,identifiers);

            }

        }



        return resultset;
    }

    private String getLeafMarker(LinkedList<QueryNode> path) {

        return path.getLast().getMarker();
    }

    private Set<String> findIdentifiers(Model model, Set<String> input, LinkedList<QueryNode> path) {


        Set<String> objects;
        Set<String> subjects;
        if (input==null)
           objects = new HashSet<>();
        else objects=input;

        Iterator<QueryNode> iterator = path.iterator();

        while (iterator.hasNext()) {
            QueryNode queryNode = iterator.next();
            subjects = new HashSet<>(objects);
            objects = new HashSet<>();
            if (!subjects.isEmpty()){
                Iterator<String> subjectIterator = subjects.iterator();
                while (subjectIterator.hasNext()) {
                    String subject = subjectIterator.next();
                    Resource subjectresoource = model.createResource(subject);
                    NodeIterator partialobjects = model.listObjectsOfProperty(subjectresoource,queryNode.getNode());
                    while(partialobjects.hasNext())
                        objects.add(partialobjects.next().toString());
                }



            }



        }
      return objects;





    }

    private boolean hasMarkerLeaf(LinkedList<QueryNode> path, Set<String> markers) {

        for (String marker : markers) {

            if(path.getLast().getMarker().equals(marker))
                return true;
        }



        return false;
    }


    private Model getModelFromRemote(JsonNode graphQlQuery) {

        //todo
        return null;
    }

    @Override
    public void setParameters(JsonNode jsonnode) {

        this.id = jsonnode.get("@id").asText();
        this.url = jsonnode.get("url").asText();
        this.user = jsonnode.get("user").asText();
        this.password = jsonnode.get("password").asText();

    }

private  Set<LinkedList<QueryNode>> getQueryPaths(JsonNode query) {
    Set<LinkedList<QueryNode>> paths = new HashSet<>() ;

    getQueryPathsRecursive(query,paths,null);
    return paths;



}

    private  void getQueryPathsRecursive(JsonNode query, Set<LinkedList<QueryNode>> paths, LinkedList<QueryNode> path)  {

        Model model= ModelFactory.createDefaultModel();

        if (path==null)
            path= new LinkedList<QueryNode>();
        else {
            paths.remove(path);
        }
            Iterator<JsonNode> iterator = query.elements();

            while (iterator.hasNext()) {
                JsonNode currentNode = iterator.next();
                LinkedList<QueryNode> newPath = new LinkedList<QueryNode>(path);
                String nodeMarker = currentNode.get("nodeId").asText();
                String nodeName = currentNode.get("name").asText();
                FieldConfig field = HGQLConfig.getInstance().fields().get(nodeName);
                if (field==null) {
                    throw  new RuntimeException("Field not found.");
                }
                Property predicate = model.createProperty(field.id());
                QueryNode queryNode = new QueryNode(predicate,nodeMarker);
                newPath.add(queryNode);
                paths.add(newPath);
                JsonNode fields = currentNode.get("fields");
                if (fields!=null&&!fields.isNull())
                getQueryPathsRecursive(fields,paths,newPath);

            }


    }
}
