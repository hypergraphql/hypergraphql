package org.hypergraphql.datafetching.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.log4j.Logger;
import org.hypergraphql.config.system.HGQLConfig;
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

    private String optionalSTR(String sparqlPattern) {
        final String PATTERN = " OPTIONAL { %s } ";
        return String.format(PATTERN, sparqlPattern);
    }

    private String selectSubquerySTR(String id, String sparqlPattern, String limit, String offset) {
        final String PATTERN = "{ SELECT " + varSTR(id) + " WHERE { %s } %s %s } ";
        return String.format(PATTERN, sparqlPattern, limit, offset);
    }

    private String selectQuerySTR(String whereSTR, String graphID) {
        final String PATTERN = "SELECT * WHERE { GRAPH <%s> { %s } } ";
        return String.format(PATTERN, graphID, whereSTR);
    }

    private String valuesSTR(String id, Set<String> input) {
        final String PATTERN = "VALUES " + varSTR(id) + " { %s } } ";
        Set<String> uris = new HashSet<>();
        for (String uri : input) uris.add(uriSTR(uri));

        String urisConcat = " { " + String.join(" ", uris) + " } ";

        return String.format(PATTERN, id, urisConcat);
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


    private String fieldPattern(String parentId, String nodeId, String predicateURI, String typeURI) {
        String predicateTriple = (parentId.equals("")) ? "" : tripleSTR(varSTR(parentId), uriSTR(predicateURI), varSTR(nodeId));
        String typeTriple = (typeURI.equals("")) ? "" : tripleSTR(varSTR(nodeId), RDF_TYPE_URI, uriSTR(typeURI));
        return predicateTriple + typeTriple;
    }


    public String getSelectQuery(JsonNode jsonQuery, String typeName, Set<String> input) {

        Boolean root = typeName.equals("Query");
        if (root) {
            return getSelectRoot(jsonQuery, typeName);
        } else {
            return getSelectNonRoot(jsonQuery, typeName, input);
        }

    }

    private String getSelectRoot(JsonNode jsonQuery, String typeName) {

        JsonNode queryField = jsonQuery.elements().next();
        JsonNode fieldSchema = config.mapping().get(typeName).get("fields").get(queryField.get("name").asText());
        String targetName = fieldSchema.get("targetName").asText();
        String targetURI = config.types().get(targetName).id();
        String graphID = ((SPARQLEndpointService) config.queryFields().get(queryField.get("name").asText()).service()).getGraph();
        String nodeId = queryField.get("nodeId").asText();
        JsonNode args = queryField.get("args");
        String limitSTR = "";
        String offsetSTR = "";
        if (args!=null) {
            if (args.has("limit")) limitSTR = limitSTR(args.get("limit").asInt());
            if (args.has("offset")) offsetSTR = offsetSTR(args.get("offset").asInt());
        }
        String selectTriple = tripleSTR(varSTR(nodeId), RDF_TYPE_URI, uriSTR(targetURI));
        String rootSubquery = selectSubquerySTR(nodeId, selectTriple, limitSTR, offsetSTR);

        JsonNode subfields = queryField.get("fields");
        String whereClause = getSubQueries(subfields, targetName);

        String selectQuery = selectQuerySTR(rootSubquery + whereClause, graphID);

        return selectQuery;
    }

    private String getSelectNonRoot(JsonNode jsonQuery, String typeName, Set<String> input) {

        JsonNode firstField = jsonQuery.elements().next();
        String graphID = ((SPARQLEndpointService) config.fields().get(firstField.get("name").asText()).service()).getGraph();
        String parentId = firstField.get("parentId").asText();
        String valueSTR = valuesSTR(parentId, input);

        Iterator<JsonNode> queryFieldsIterator = jsonQuery.elements();

        String whereClause = "";

        while (queryFieldsIterator.hasNext()) {

            JsonNode field = queryFieldsIterator.next();

            String subquery = getFieldSubquery(field, typeName);

            whereClause += subquery;
        }

        String selectQuery = selectQuerySTR(valueSTR + whereClause, graphID);

        return selectQuery;
    }


    private String getFieldSubquery(JsonNode fieldJson, String typeName) {

        String fieldName = fieldJson.get("name").asText();

        if (config.getJSONLD_VOC().containsKey(fieldName)) return "";

        String fieldURI = config.fields().get(fieldName).id();
        JsonNode fieldSchema = config.mapping().get(typeName).get("fields").get(fieldJson.get("name").asText());
        String targetName = fieldSchema.get("targetName").asText();
        String parentId = fieldJson.get("parentId").asText();
        String nodeId = fieldJson.get("nodeId").asText();
        JsonNode args = fieldJson.get("args");

        String langFilter = langFilterSTR(fieldJson);

        String typeURI = (config.types().containsKey(targetName)) ? config.types().get(targetName).id() : "";

        String fieldPattern = fieldPattern(parentId, nodeId, fieldURI, typeURI);

        JsonNode subfields = fieldJson.get("fields");

        String rest = getSubQueries(subfields, targetName);

        String whereClause = optionalSTR(fieldPattern + langFilter + rest);

        return whereClause;
    }


    private String getSubQueries(JsonNode subfields, String typeName) {

        Iterator<JsonNode> queryFieldsIterator = subfields.elements();

        String whereClause = "";

        while (queryFieldsIterator.hasNext()) {

            JsonNode field = queryFieldsIterator.next();

            whereClause += getFieldSubquery(field, typeName);

        }

        return whereClause;

    }

}
