package org.hypergraphql.datafetching.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

    private String graphPattern(String fieldPattern, JsonNode field, JsonNode parentField) {
        JsonNode args = field.get("args");
        String graphName = (args.has("graph")) ? args.get("graph").asText() : field.get("graph").asText();

        String graphPattern;
        if (parentField != null) {
            JsonNode parentArgs = parentField.get("args");
            String parentGraphName = (parentArgs.has("graph")) ? parentArgs.get("graph").asText() : parentField.get("graph").asText();
            graphPattern = (parentGraphName.equals(graphName)) ? fieldPattern : graphSTR(graphName, fieldPattern);
        } else {
            graphPattern = graphSTR(graphName, fieldPattern);
        }
        return graphPattern;
    }

    //from graphqlConfig names to jsonld reserved names
    public String getSelectQuery(JsonNode jsonQuery) {

        Boolean root = jsonQuery.elements().next().get("parentID")==null;
        if (root) {
            return getSelectRoot(jsonQuery);
        } else {
            return getSelectNonRoot(jsonQuery);
        }

    }

    private String getSelectRoot(JsonNode jsonQuery) {

        JsonNode queryField = jsonQuery.elements().next();
        JsonNode fieldSchema = config.mapping().get("Query").get("fields").get(queryField.get("name").asText());
        String targetName = fieldSchema.get("targetName").asText();
        String targetURI = config.types().get(targetName).id();
        String graphID = config.queryFields().get(queryField.get("name").asText()).service().getGraph();
        String nodeId = queryField.get("nodeId").asText();
        JsonNode args = queryField.get("args");
        if (args!=null) {
            int limit = (args.has("limit")) ? args.get("limit") :
        }

        JsonNode subfields = queryField.get("fields");



        String whereClause = getSubquery(subfields);

        String selectQuery = selectQuerySTR(whereClause, graphID);

        return selectQuery;
    }

    public String getSubqueryRoot(JsonNode jsonQuery) {

        JsonNode queryField = jsonQuery.elements().next();
        JsonNode fieldSchema = config.mapping().get("Query").get("fields").get(queryField.get("name").asText());
        String targetName = fieldSchema.get("targetName").asText();

        config.types().get(targetName).id();

    }

    private Map<String, String> getConstructQuery(JsonNode query, Boolean rootQuery) {

        Map<String, String> output = new HashMap<>();
        String service = query.fieldNames().next();
        output.put("service", service);

        Iterator<JsonNode> fields = query.get(service).elements();

        String constructPattern = "";
        String wherePattern = "";

        String matchParent;

        while (fields.hasNext()) {
            JsonNode field = fields.next();

            JsonNode args = field.get("args");
            String nodeId = field.get("nodeId").asText();
            String parentId = (field.has("parentId")) ? field.get("parentId").asText() : "";
            String fieldPattern = fieldPattern(field);

            matchParent = markTripleSTR(parentId);
            String queryName = (field.has("alias")) ? field.get("alias").asText() : field.get("name").asText();
            String rootTriple = (rootQuery) ? rootTripleSTR(nodeId, queryName) : "";
            String markTriple = (field.has("fields")) ? markTripleSTR(nodeId) : "";

            String limit = (args.has("limit")) ? limitSTR(args.get("limit").asInt()) : "";
            String offset = (args.has("offset")) ? offsetSTR(args.get("offset").asInt()) : "";

            Map<String, String> subqueries = getSubqueries(service, field);

            String subConstruct = (subqueries.containsKey("construct")) ? subqueries.get("construct") : "";
            String subWhere = (subqueries.containsKey("where")) ? subqueries.get("where") : "";

            String matchPattern = (rootQuery) ? selectSTR(nodeId, fieldPattern, limit, offset) + subWhere + wherePattern : fieldPattern + subWhere + wherePattern;

            constructPattern = fieldPattern + rootTriple + markTriple + subConstruct + constructPattern;
            wherePattern = graphPattern(matchPattern, field, null);

            output.put("match", matchParent);
            output.put("var", varSTR(parentId));
        }

        wherePattern = (rootQuery) ? wherePattern : "%s " + wherePattern;
        // String servicePattern = serviceSTR(service, wherePattern);
        String constructQuery = constructSTR(constructPattern, wherePattern);

        output.put("query", constructQuery);

        return output;
    }
}
