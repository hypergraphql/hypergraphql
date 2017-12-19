package org.hypergraphql.query.converters;

import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.hypergraphql.config.schema.QueryFieldConfig;

import org.hypergraphql.datamodel.HGQLSchema;

import org.hypergraphql.config.schema.HGQLVocabulary;
import org.hypergraphql.datafetching.services.SPARQLEndpointService;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SPARQLServiceConverter {


    private final HGQLSchema schema;

    public SPARQLServiceConverter(HGQLSchema schema) {
        this.schema = schema;
    }

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
        String result = (graphID==null || graphID.equals("")) ? whereSTR : String.format(PATTERN, graphID, whereSTR);
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


    public String getSelectQuery(JsonNode jsonQuery, Set<String> input, String rootType) {

        Map<String, QueryFieldConfig> queryFields = schema.getQueryFields();

        Boolean root = (!jsonQuery.isArray() && queryFields.containsKey(jsonQuery.get("name").asText()));

        if (root) {
            if (queryFields.get(jsonQuery.get("name").asText()).type().equals(HGQLVocabulary.HGQL_QUERY_GET_FIELD)) {
                return getSelectRoot_GET(jsonQuery);
            } else {
                return getSelectRoot_GET_BY_ID(jsonQuery);
            }
        } else {
            return getSelectNonRoot((ArrayNode) jsonQuery, input, rootType);
        }
    }

    private String getSelectRoot_GET_BY_ID(JsonNode queryField) {

        Iterator<JsonNode> urisIter = queryField.get("args").get("uris").elements();

        Set<String> uris = new HashSet<>();

        urisIter.forEachRemaining(uri -> uris.add(uri.asText()));

        String targetName = queryField.get("targetName").asText();
        String targetURI = schema.getTypes().get(targetName).getId();
        String graphID = ((SPARQLEndpointService) schema.getQueryFields().get(queryField.get("name").asText()).service()).getGraph();
        String nodeId = queryField.get("nodeId").asText();
        String selectTriple = tripleSTR(varSTR(nodeId), RDF_TYPE_URI, uriSTR(targetURI));
        String valueSTR = valuesSTR(nodeId, uris);

        JsonNode subfields = queryField.get("fields");
        String subQuery = getSubQueries(subfields);

        String selectQuery = selectQuerySTR(valueSTR + selectTriple + subQuery, graphID);

        return selectQuery;
    }

    private String getSelectRoot_GET(JsonNode queryField) {

        String targetName = queryField.get("targetName").asText();
        String targetURI = schema.getTypes().get(targetName).getId();
        String graphID = ((SPARQLEndpointService) schema.getQueryFields().get(queryField.get("name").asText()).service()).getGraph();
        String nodeId = queryField.get("nodeId").asText();
        String limitOffsetSTR = limitOffsetSTR(queryField);
        String selectTriple = tripleSTR(varSTR(nodeId), RDF_TYPE_URI, uriSTR(targetURI));
        String rootSubquery = selectSubquerySTR(nodeId, selectTriple, limitOffsetSTR);

        JsonNode subfields = queryField.get("fields");
        String whereClause = getSubQueries(subfields);

        String selectQuery = selectQuerySTR(rootSubquery + whereClause, graphID);

        return selectQuery;
    }


    private String getSelectNonRoot(ArrayNode jsonQuery, Set<String> input, String rootType) {


        JsonNode firstField = jsonQuery.elements().next();
        String graphID = ((SPARQLEndpointService) schema.getTypes().get(rootType).getFields().get(firstField.get("name").asText()).getService()).getGraph();
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

        if (HGQLVocabulary.JSONLD.containsKey(fieldName)) return "";

        String fieldURI = schema.getFields().get(fieldName).getId();
        String targetName = fieldJson.get("targetName").asText();
        String parentId = fieldJson.get("parentId").asText();
        String nodeId = fieldJson.get("nodeId").asText();

        String langFilter = langFilterSTR(fieldJson);

        String typeURI = (schema.getTypes().containsKey(targetName)) ? schema.getTypes().get(targetName).getId() : "";

        String fieldPattern = fieldPattern(parentId, nodeId, fieldURI, typeURI);

        JsonNode subfields = fieldJson.get("fields");

        String rest = getSubQueries(subfields);



        String whereClause = optionalSTR(fieldPattern + langFilter + rest);

        return whereClause;
    }


    private String getSubQueries(JsonNode subfields) {

        if (subfields.isNull()) return "";

        Iterator<JsonNode> queryFieldsIterator = subfields.elements();

        String whereClause = "";

        while (queryFieldsIterator.hasNext()) {

            JsonNode field = queryFieldsIterator.next();

            whereClause += getFieldSubquery(field);

        }

        return whereClause;

    }


}
