package org.hypergraphql.datafetching.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.log4j.Logger;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.datafetching.TreeExecutionResult;
import org.hypergraphql.query.Converter;

import java.util.*;

public abstract class SPARQLService extends Service {

    protected String graph;

    public String getGraph() {
        return graph;
    }

    public void setGraph(String graph) {
        this.graph = graph;
    }


    public  void setParameters(JsonNode jsonnode) {

        this.graph = jsonnode.get("graph").asText();
    }


    static Logger logger = Logger.getLogger(Converter.class);

    private HGQLConfig config;

    private final String RDF_TYPE_URI = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";

    private String graphSTR(String graph, String triple) {
        final String PATTERN = "GRAPH <%s> { %s } ";
        return (graph.equals("")) ? triple : String.format(PATTERN, graph, triple);
    }

    private String optionalSTR(String triple, String sparqlPattern) {
        final String PATTERN = " OPTIONAL { %s %s } ";
        return String.format(PATTERN, triple, sparqlPattern);
    }

    private String selectSubquerySTR(String id, String sparqlPattern, String limit, String offset) {
        final String PATTERN = "{ SELECT " + varSTR(id) + " WHERE { %s } %s %s } ";
        return String.format(PATTERN, sparqlPattern, limit, offset);
    }

    private String selectQuerySTR(String whereSTR, String graphID) {
        final String PATTERN = "SELECT * WHERE { GRAPH <%s> { %s } } ";
        return String.format(PATTERN, graphID, whereSTR);
    }

    private String limitSTR(int no) {
        final String PATTERN = "LIMIT %s ";
        return String.format(PATTERN, no);
    }

    private String offsetSTR(int no) {
        final String PATTERN = "OFFSET %s ";
        return String.format(PATTERN, no);
    }

    private String uriSTR(String uri) {
        final String PATTERN = "<%s>";
        return String.format(PATTERN, uri);
    }

    private String varSTR(String id) {
        final String PATTERN = "?%s";
        return String.format(PATTERN, id);
    }



    private String tripleSTR(String subject, String predicate, String object) {
        final String PATTERN = "%s %s %s . ";
        return String.format(PATTERN, subject, predicate, object);
    }

    private String langFilterSTR(JsonNode field) {
        final String PATTERN = "FILTER (lang(%s) = \"%s\") . ";
        String nodeVar = varSTR(field.get("nodeId").asText());
        JsonNode args = field.get("args");
        String langPattern = (args.has("lang")) ? String.format(PATTERN, nodeVar, args.get("lang").asText()) : "";
        return langPattern;
    }

    private String constructSTR(String constructPattern, String wherePattern) {
        final String PATTERN = "CONSTRUCT { %s } WHERE { %s }";
        return String.format(PATTERN, constructPattern, wherePattern);
    }


    private String fieldPattern(String parentId, String nodeId, String predicateURI, String typeURI) {
        String predicateTriple = (parentId.equals("")) ? "" : tripleSTR(varSTR(parentId), uriSTR(predicateURI), varSTR(nodeId));
        String typeTriple = (typeURI.equals("")) ? "" : tripleSTR(varSTR(nodeId), RDF_TYPE_URI, uriSTR(typeURI));
        return predicateTriple + typeTriple;
    }

    private String fieldPattern(JsonNode field) {
        String nodeId = field.get("nodeId").asText();
        String parentId = (field.has("parentId")) ? field.get("parentId").asText() : "";
        String predicateURI = (field.has("uri")) ? field.get("uri").asText() : "";
        String typeURI = (field.has("targetURI")) ? field.get("targetURI").asText() : "";
        return fieldPattern(parentId, nodeId, predicateURI, typeURI);
    }


//    //from graphqlConfig names to jsonld reserved names
//    public String getSelectQuery(JsonNode jsonQuery) {
//
//        Boolean root = jsonQuery.elements().next().get("parentID")==null;
//        if (root) {
//            return getSelectRoot(jsonQuery);
//        } else {
//            return getSelectNonRoot(jsonQuery);
//        }
//
//    }

//    private String getSelectRoot(JsonNode jsonQuery) {
//
//        JsonNode queryField = jsonQuery.elements().next();
//        JsonNode fieldSchema = config.mapping().get("Query").get("fields").get(queryField.get("name").asText());
//        String targetName = fieldSchema.get("targetName").asText();
//        String targetURI = config.types().get(targetName).id();
//        String graphID = ((SPARQLEndpointService) config.queryFields().get(queryField.get("name").asText()).service()).getGraph();
//        String nodeId = queryField.get("nodeId").asText();
//        JsonNode args = queryField.get("args");
//        String limitSTR = "";
//        String offsetSTR = "";
//        if (args!=null) {
//            if (args.has("limit")) limitSTR = limitSTR(args.get("limit").asInt());
//            if (args.has("offset")) offsetSTR = offsetSTR(args.get("offset").asInt());
//        }
//        String selectTriple = tripleSTR(varSTR(nodeId), RDF_TYPE_URI, uriSTR(targetURI));
//        String rootSubquery = selectSubquerySTR(nodeId, selectTriple, limitSTR, offsetSTR);
//
//        JsonNode subfields = queryField.get("fields");
//        String whereClause = getSubQueries(subfields, targetName);
//
//        String selectQuery = selectQuerySTR(rootSubquery + whereClause, graphID);
//
//        return selectQuery;
//    }
//
//    private String getSelectNonRoot(JsonNode jsonQuery, String typeName) {
//
//        JsonNode firstField = jsonQuery.elements().next();
//        String graphID = ((SPARQLEndpointService) config.fields().get(firstField.get("name").asText()).service()).getGraph();
//
//        Iterator<JsonNode> queryFieldsIterator = jsonQuery.elements();
//
//        String whereClause = "";
//        while (queryFieldsIterator.hasNext()) {
//            JsonNode field = queryFieldsIterator.next();
//            String fieldURI = config.fields().get(field.get("name").asText()).id();
//            JsonNode fieldSchema = config.mapping().get(typeName).get("fields").get(field.get("name").asText());
//            String targetName = fieldSchema.get("targetName").asText();
//            String nodeId = field.get("nodeId").asText();
//            JsonNode args = field.get("args");
//
//            String targetURI = (config.types().get(targetName).id();
//
//
//
//        }
//
//    }
//
//    private String getFieldSubquery(JsonNode fieldJson, String typeName) {
//
//        String fieldURI = config.fields().get(fieldJson.get("name").asText()).id();
//        JsonNode fieldSchema = config.mapping().get(typeName).get("fields").get(fieldJson.get("name").asText());
//        String targetName = fieldSchema.get("targetName").asText();
//        String parentId = fieldJson.get("parentId").asText();
//        String nodeId = fieldJson.get("nodeId").asText();
//        JsonNode args = fieldJson.get("args");
//
//        String typeURI = (config.types().containsKey(targetName)) ? config.types().get(targetName).id() : "";
//
//        String fieldPattern = fieldPattern(parentId, nodeId, fieldURI, typeURI);
//
//        JsonNode subfields = fieldJson.get("fields");
//
//        String rest = getSubQueries(subfields, targetName);
//
//        String whereClause = optionalSTR(fieldPattern, rest);
//
//        return whereClause;
//    }

}
