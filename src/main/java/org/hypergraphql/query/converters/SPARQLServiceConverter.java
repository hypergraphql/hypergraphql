package org.hypergraphql.query.converters;

import com.fasterxml.jackson.databind.JsonNode;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.datafetching.services.SPARQLEndpointService;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class SPARQLServiceConverter {

    private HGQLConfig config = HGQLConfig.getInstance();

    private final String RDF_TYPE_URI = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";

    private String optionalSTR(String sparqlPattern) {
        final String PATTERN = " OPTIONAL { %s } ";
        return String.format(PATTERN, sparqlPattern);
    }

    private String selectSubquerySTR(String id, String sparqlPattern, String limitOffset) {
        final String PATTERN = "{ SELECT " + varSTR(id) + " WHERE { %s } %s } ";
        return String.format(PATTERN, sparqlPattern, limitOffset);
    }

    private String selectQuerySTR(String whereSTR, String graphID) {
        final String PATTERN = "SELECT * WHERE { %s } ";
        return String.format(PATTERN, graphSTR(graphID, whereSTR));
    }

    private String graphSTR(String graphID, String whereSTR) {
        final String PATTERN = "GRAPH <%s> { %s } ";
        String result = (graphID.equals("")) ? whereSTR : String.format(PATTERN, graphID, whereSTR);
        return result;
    }

    private String valuesSTR(String id, Set<String> input) {
        final String PATTERN = "VALUES " + varSTR(id) + " { %s } ";
        Set<String> uris = new HashSet<>();
        for (String uri : input) uris.add(uriSTR(uri));

        String urisConcat = String.join(" ", uris);

        return String.format(PATTERN, urisConcat);
    }

    private String limitOffsetSTR(JsonNode jsonQuery) {
        JsonNode args = jsonQuery.get("args");
        String limitSTR = "";
        String offsetSTR = "";
        if (args!=null) {
            if (args.has("limit")) limitSTR = limitSTR(args.get("limit").asInt());
            if (args.has("offset")) offsetSTR = offsetSTR(args.get("offset").asInt());
        }
        return limitSTR + offsetSTR;
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


    public String getSelectQuery(JsonNode jsonQuery, Set<String> input) {

        Boolean root = input.isEmpty();
        if (root) {
            return getSelectRoot(jsonQuery);
        } else {
            return getSelectNonRoot(jsonQuery, input);
        }

    }

    private String getSelectRoot(JsonNode queryField) {

        String targetName = queryField.get("targetName").asText();
        String targetURI = config.types().get(targetName).id();
        String graphID = ((SPARQLEndpointService) config.queryFields().get(queryField.get("name").asText()).service()).getGraph();
        String nodeId = queryField.get("nodeId").asText();
        String limitOffsetSTR = limitOffsetSTR(queryField);
        String selectTriple = tripleSTR(varSTR(nodeId), RDF_TYPE_URI, uriSTR(targetURI));
        String rootSubquery = selectSubquerySTR(nodeId, selectTriple, limitOffsetSTR);

        JsonNode subfields = queryField.get("fields");
        String whereClause = getSubQueries(subfields);

        String selectQuery = selectQuerySTR(rootSubquery + whereClause, graphID);

        return selectQuery;
    }

    private String getSelectNonRoot(JsonNode jsonQuery, Set<String> input) {

        JsonNode firstField = jsonQuery.elements().next();
        String graphID = ((SPARQLEndpointService) config.fields().get(firstField.get("name").asText()).service()).getGraph();
        String parentId = firstField.get("parentId").asText();
        String valueSTR = valuesSTR(parentId, input);

        Iterator<JsonNode> queryFieldsIterator = jsonQuery.elements();

        String whereClause = "";

        while (queryFieldsIterator.hasNext()) {

            JsonNode field = queryFieldsIterator.next();

            String subquery = getFieldSubquery(field);

            whereClause += subquery;
        }

        String selectQuery = selectQuerySTR(valueSTR + whereClause, graphID);

        return selectQuery;
    }


    private String getFieldSubquery(JsonNode fieldJson) {

        String fieldName = fieldJson.get("name").asText();

        if (config.getJSONLD_VOC().containsKey(fieldName)) return "";

        String fieldURI = config.fields().get(fieldName).id();
        String targetName = fieldJson.get("targetName").asText();
        String parentId = fieldJson.get("parentId").asText();
        String nodeId = fieldJson.get("nodeId").asText();

        String langFilter = langFilterSTR(fieldJson);

        String typeURI = (config.types().containsKey(targetName)) ? config.types().get(targetName).id() : "";

        String fieldPattern = fieldPattern(parentId, nodeId, fieldURI, typeURI);

        JsonNode subfields = fieldJson.get("fields");

        String rest = getSubQueries(subfields);

        String whereClause = optionalSTR(fieldPattern + langFilter + rest);

        return whereClause;
    }


    private String getSubQueries(JsonNode subfields) {

        Iterator<JsonNode> queryFieldsIterator = subfields.elements();

        String whereClause = "";

        while (queryFieldsIterator.hasNext()) {

            JsonNode field = queryFieldsIterator.next();

            whereClause += getFieldSubquery(field);

        }

        return whereClause;

    }


}
