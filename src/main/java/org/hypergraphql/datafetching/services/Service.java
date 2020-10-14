package org.hypergraphql.datafetching.services;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;
import org.hypergraphql.config.schema.FieldConfig;
import org.hypergraphql.config.schema.TypeConfig;
import org.hypergraphql.config.system.ServiceConfig;
import org.hypergraphql.datafetching.TreeExecutionResult;
import org.hypergraphql.datamodel.HGQLSchema;
import org.hypergraphql.datamodel.QueryNode;

import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_QUERY_NAMESPACE;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_QUERY_URI;
import static org.hypergraphql.config.schema.HGQLVocabulary.RDF_TYPE;

public abstract class Service {

    private String type;
    private String id;

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public abstract TreeExecutionResult executeQuery(JsonNode query, Set<String> input, Set<String> strings, String rootType, HGQLSchema schema);

    public abstract void setParameters(ServiceConfig serviceConfig);

    public Model getModelFromResults(final JsonNode query,
                                     final QuerySolution results,
                                     final HGQLSchema schema) {

        final var model = ModelFactory.createDefaultModel();
        if (query.isNull()) {
            return model;
        }

        if (query.isArray()) {

            final Iterator<JsonNode> nodesIterator = query.elements();

            while (nodesIterator.hasNext()) {
                final var currentNode = nodesIterator.next();
                final var currentModel = buildModel(results, currentNode, schema);
                model.add(currentModel);
                model.add(getModelFromResults(currentNode.get("fields"), results, schema));
            }
        } else {
            final var currentModel = buildModel(results, query, schema);
            model.add(currentModel);
            model.add(getModelFromResults(query.get("fields"), results, schema));
        }
        return model;

    }

    private Model buildModel(final QuerySolution results,
                             final JsonNode currentNode,
                             final HGQLSchema schema) {

        final var model = ModelFactory.createDefaultModel();

        final var propertyString = schema.getFields().get(currentNode.get("name").asText());
        final var targetTypeString = schema.getTypes().get(currentNode.get("targetName").asText());

        populateModel(results, currentNode, model, propertyString, targetTypeString);

        final var queryField = schema.getQueryFields().get(currentNode.get("name").asText());

        if (queryField != null) {

            final var typeName = (currentNode.get("alias").isNull()) ? currentNode.get("name").asText() : currentNode.get("alias").asText();
            final var object = results.getResource(currentNode.get("nodeId").asText());
            final var subject = model.createResource(HGQL_QUERY_URI);
            final var predicate = model.createProperty("", HGQL_QUERY_NAMESPACE + typeName);
            model.add(subject, predicate, object);
        }
        return model;
    }

    Map<String, Set<String>> getResultset(final Model model,
                                          final JsonNode query,
                                          final Set<String> input,
                                          final Set<String> markers,
                                          final HGQLSchema schema) {

        final Map<String, Set<String>> resultset = new HashMap<>();
        final JsonNode node;

        if (query.isArray()) {
            node = query; // TODO - in this situation, we should iterate over the array
        } else {
            node = query.get("fields");
            if (markers.contains(query.get("nodeId").asText())) {
                resultset.put(query.get("nodeId").asText(), findRootIdentifiers(model, schema.getTypes().get(query.get("targetName").asText())));
            }
        }
        Set<LinkedList<QueryNode>> paths = new HashSet<>(); // TODO - variable reuse
        if (node != null && !node.isNull()) {
            paths = getQueryPaths(node, schema);
        }

        paths.forEach(path -> {
            if (hasMarkerLeaf(path, markers)) {
                Set<String> identifiers = findIdentifiers(model, input, path);
                String marker = getLeafMarker(path);
                resultset.put(marker, identifiers);
            }
        });

        // TODO query happens to be an array sometimes - then the following line fails.

        return resultset;
    }

    private Set<String> findRootIdentifiers(final Model model, final TypeConfig targetName) {

        final Set<String> identifiers = new HashSet<>();
        final var currentModel = ModelFactory.createDefaultModel();
        final var res = currentModel.createResource(targetName.getId());
        final var property = currentModel.createProperty(RDF_TYPE);

        final var iterator = model.listResourcesWithProperty(property, res);

        while (iterator.hasNext()) {
            identifiers.add(iterator.nextResource().toString());
        }
        return identifiers;
    }

    private String getLeafMarker(final LinkedList<QueryNode> path) {

        return path.getLast().getMarker();
    }

    private Set<String> findIdentifiers(final Model model,
                                        final Set<String> input,
                                        final LinkedList<QueryNode> path) {

        Set<String> subjects; // TODO - variable reuse
        Set<String> objects; // TODO - variable reuse
        if (input == null) {
            objects = new HashSet<>();
        } else {
            objects = input;
        }

        // NB: This hasn't been converted to use the NIO streaming API as it uses reentrant recursion
        for (final QueryNode queryNode : path) {
            subjects = new HashSet<>(objects);
            objects = new HashSet<>();
            if (!subjects.isEmpty()) {
                for (final String subject : subjects) {
                    final var subjectResource = model.createResource(subject);
                    final var partialObjects = model.listObjectsOfProperty(subjectResource, queryNode.getNode());
                    while (partialObjects.hasNext()) {
                        objects.add(partialObjects.next().toString());
                    }
                }

            } else {

                final var objectsIterator = model.listObjectsOfProperty(queryNode.getNode());
                while (objectsIterator.hasNext()) {
                    objects.add(objectsIterator.next().toString());
                }
            }
        }
        return objects;
    }

    private boolean hasMarkerLeaf(final LinkedList<QueryNode> path, final Set<String> markers) {

        for (final String marker : markers) {
            if (path.getLast().getMarker().equals(marker)) {
                return true;
            }
        }
        return false;
    }

    private Set<LinkedList<QueryNode>> getQueryPaths(final JsonNode query, final HGQLSchema schema) {
        final Set<LinkedList<QueryNode>> paths = new HashSet<>();
        getQueryPathsRecursive(query, paths, null ,  schema);
        return paths;
    }

    private void getQueryPathsRecursive(final JsonNode query,
                                        final Set<LinkedList<QueryNode>> paths,
                                        LinkedList<QueryNode> path,
                                        final HGQLSchema schema) {

        Model model = ModelFactory.createDefaultModel();

        if (path == null) {
            path = new LinkedList<>();
        } else {
            paths.remove(path);
        }

        if (query.isArray()) {
            final Iterator<JsonNode> iterator = query.elements();

            while (iterator.hasNext()) {
                final var currentNode = iterator.next();
                getFieldPath(paths, path, schema, model, currentNode);
            }
        } else {
            getFieldPath(paths, path, schema, model, query);
        }
    }

    private void getFieldPath(final Set<LinkedList<QueryNode>> paths,
                              final LinkedList<QueryNode> path,
                              final HGQLSchema schema,
                              final Model model,
                              final JsonNode currentNode) {

        final LinkedList<QueryNode> newPath = new LinkedList<>(path);
        final var nodeMarker = currentNode.get("nodeId").asText();
        final var nodeName = currentNode.get("name").asText();
        final var field = schema.getFields().get(nodeName);
        if (field == null) {
            throw new RuntimeException("field not found");
        }

        final var predicate = model.createProperty(field.getId());
        final var queryNode = new QueryNode(predicate, nodeMarker);
        newPath.add(queryNode);
        paths.add(newPath);
        final var fields = currentNode.get("fields");
        if (fields != null && !fields.isNull()) {
            getQueryPathsRecursive(fields, paths, newPath, schema);
        }
    }

    private void populateModel(
            final QuerySolution results,
            final JsonNode currentNode,
            final Model model,
            final FieldConfig propertyString,
            final TypeConfig targetTypeString
    ) {

        if (propertyString != null && !(currentNode.get("parentId").asText().equals("null"))) {
            final var predicate = model.createProperty("", propertyString.getId());
            final var subject = results.getResource(currentNode.get("parentId").asText());
            final var object = results.get(currentNode.get("nodeId").asText());
            if (predicate != null && subject != null && object != null) {
                model.add(subject, predicate, object);
            }
        }

        if (targetTypeString != null) {
            final var subject = results.getResource(currentNode.get("nodeId").asText());
            final var object = model.createResource(targetTypeString.getId());
            if (subject != null && object != null) {
                model.add(subject, RDF.type, object);
            }
        }
    }
}




