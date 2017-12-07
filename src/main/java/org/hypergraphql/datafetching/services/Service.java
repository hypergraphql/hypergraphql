package org.hypergraphql.datafetching.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.hypergraphql.config.system.ServiceConfig;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.hypergraphql.config.schema.FieldConfig;
import org.hypergraphql.config.schema.QueryFieldConfig;
import org.hypergraphql.config.schema.TypeConfig;
import org.hypergraphql.datafetching.TreeExecutionResult;
import org.hypergraphql.datamodel.HGQLSchemaWiring;
import org.hypergraphql.datamodel.QueryNode;

import java.util.*;

import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_QUERY_NAMESPACE;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_QUERY_URI;

public abstract class Service {

    protected String type;
    protected String id;


    protected Class SPARQLEndpointService;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public abstract TreeExecutionResult executeQuery(JsonNode query, Set<String> input, Set<String> strings);

    public abstract void setParameters(ServiceConfig serviceConfig);

    public Model getModelFromResults(JsonNode query, QuerySolution results) {

        Model model = ModelFactory.createDefaultModel();
        if (query.isNull()) return model;

        if (query.isArray()) {


            Iterator<JsonNode> nodesIterator = query.elements();

            while (nodesIterator.hasNext()) {


                JsonNode currentNode = nodesIterator.next();

                Model currentmodel = buildmodel(results, currentNode);
                model.add(currentmodel);
                model.add(getModelFromResults(currentNode.get("fields"), results));

            }
        } else {

            Model currentModel = buildmodel(results, query);
            model.add(currentModel);
            model.add(getModelFromResults(query.get("fields"), results));

        }

        return model;

    }

    private Model buildmodel(QuerySolution results, JsonNode currentNode) {


        HGQLSchemaWiring wiring = HGQLSchemaWiring.getInstance();

        Model model = ModelFactory.createDefaultModel();

        FieldConfig propertyString = wiring.getFields().get(currentNode.get("name").asText());
        TypeConfig targetTypeString = wiring.getTypes().get(currentNode.get("targetName").asText());

        if (propertyString != null && !(currentNode.get("parentId").asText().equals("null"))) {
            Property predicate = model.createProperty("", propertyString.getId());
            Resource subject = results.getResource(currentNode.get("parentId").asText());
            RDFNode object = results.get(currentNode.get("nodeId").asText());
            if (predicate != null && subject != null && object != null)
                model.add(subject, predicate, object);
        }

        if (targetTypeString != null) {
            Resource subject = results.getResource(currentNode.get("nodeId").asText());
            Resource object = model.createResource(targetTypeString.getId());
            if (subject != null && object != null)
                model.add(subject, RDF.type, object);
        }

        QueryFieldConfig queryField = wiring.getQueryFields().get(currentNode.get("name").asText());

        if (queryField != null) {

            String typeName = (currentNode.get("alias").isNull()) ? currentNode.get("name").asText() : currentNode.get("alias").asText();
            Resource object = results.getResource(currentNode.get("nodeId").asText());
            Resource subject = model.createResource(HGQL_QUERY_URI);
            Property predicate = model.createProperty("", HGQL_QUERY_NAMESPACE + typeName);
            model.add(subject, predicate, object);
        }
        return model;
    }

    protected Map<String, Set<String>> getResultset(Model model, JsonNode query, Set<String> input, Set<String> markers) {


        Map<String, Set<String>> resultset = new HashMap<>();

        Set<LinkedList<QueryNode>> paths = getQueryPaths(query);

        for (LinkedList<QueryNode> path : paths) {

            if (hasMarkerLeaf(path, markers)) {
                Set<String> identifiers = findIdentifiers(model, input, path);
                String marker = getLeafMarker(path);
                resultset.put(marker, identifiers);

            }

        }


        return resultset;
    }

    protected String getLeafMarker(LinkedList<QueryNode> path) {

        return path.getLast().getMarker();
    }

    protected Set<String> findIdentifiers(Model model, Set<String> input, LinkedList<QueryNode> path) {


        Set<String> objects;
        Set<String> subjects;
        if (input == null)
            objects = new HashSet<>();
        else objects = input;

        Iterator<QueryNode> iterator = path.iterator();

        while (iterator.hasNext()) {
            QueryNode queryNode = iterator.next();
            subjects = new HashSet<>(objects);
            objects = new HashSet<>();
            if (!subjects.isEmpty()) {
                Iterator<String> subjectIterator = subjects.iterator();
                while (subjectIterator.hasNext()) {
                    String subject = subjectIterator.next();
                    Resource subjectresoource = model.createResource(subject);
                    NodeIterator partialobjects = model.listObjectsOfProperty(subjectresoource, queryNode.getNode());
                    while (partialobjects.hasNext())
                        objects.add(partialobjects.next().toString());
                }

            } else {

                NodeIterator objectsIterator = model.listObjectsOfProperty(queryNode.getNode());
                while (objectsIterator.hasNext())
                    objects.add(objectsIterator.next().toString());


            }


        }
        return objects;


    }

    protected boolean hasMarkerLeaf(LinkedList<QueryNode> path, Set<String> markers) {

        for (String marker : markers) {

            if (path.getLast().getMarker().equals(marker))
                return true;
        }


        return false;
    }

    protected Set<LinkedList<QueryNode>> getQueryPaths(JsonNode query) {
        Set<LinkedList<QueryNode>> paths = new HashSet<>();

        getQueryPathsRecursive(query, paths, null);
        return paths;


    }

    protected void getQueryPathsRecursive(JsonNode query, Set<LinkedList<QueryNode>> paths, LinkedList<QueryNode> path) {

        Model model = ModelFactory.createDefaultModel();

        if (path == null)
            path = new LinkedList<QueryNode>();
        else {
            paths.remove(path);
        }
        Iterator<JsonNode> iterator = query.elements();

        while (iterator.hasNext()) {
            JsonNode currentNode = iterator.next();
            LinkedList<QueryNode> newPath = new LinkedList<QueryNode>(path);
            String nodeMarker = currentNode.get("nodeId").asText();
            String nodeName = currentNode.get("name").asText();
            FieldConfig field = HGQLSchemaWiring.getInstance().getFields().get(nodeName);
            if (field == null) {
                throw new RuntimeException("Field not found.");
            }
            Property predicate = model.createProperty(field.getId());
            QueryNode queryNode = new QueryNode(predicate, nodeMarker);
            newPath.add(queryNode);
            paths.add(newPath);
            JsonNode fields = currentNode.get("fields");
            if (fields != null && !fields.isNull())
                getQueryPathsRecursive(fields, paths, newPath);

        }


    }
}




