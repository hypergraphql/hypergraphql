package org.hypergraphql.query.converters;

import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.lang3.StringUtils;
import org.hypergraphql.config.schema.QueryFieldConfig;

import org.hypergraphql.datamodel.HGQLSchema;

import org.hypergraphql.config.schema.HGQLVocabulary;
import org.hypergraphql.datafetching.services.SPARQLEndpointService;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SPARQLServiceConverter {

    private final static String RDF_TYPE_URI = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
    private final static String NAME = "name";
    private final static String URIS = "uris";
    private final static String NODE_ID = "nodeId";
    private final static String LANG = "lang";
    private final static String FIELDS = "fields";
    private final static String ARGS = "args";
    private final static String TARGET_NAME = "targetName";
    private final static String PARENT_ID = "parentId";
    private final static String LIMIT = "limit";
    private final static String OFFSET = "offset";

    private final HGQLSchema schema;

    public SPARQLServiceConverter(HGQLSchema schema) {
        this.schema = schema;
    }

    private String optionalClause(String sparqlPattern) {
        return " OPTIONAL { " + sparqlPattern + " } ";
    }

    private String selectSubqueryClause(String id, String sparqlPattern, String limitOffset) {
        return "{ SELECT " + toVar(id) + " WHERE { " + sparqlPattern + " } " + limitOffset + " } ";
    }

    private String selectQueryClause(String where, String graphID) {
        return  "SELECT * WHERE { " + graphClause(graphID, where) + " } ";
    }

    private String graphClause(String graphID, String where) {
        if (StringUtils.isEmpty(graphID)) {
            return where;
        } else {
            return "GRAPH <" + graphID + "> { " + where + " } ";
        }
    }

    private String valuesClause(String id, Set<String> input) {
        String var = toVar(id);
        Set<String> uris = new HashSet<>();
        input.forEach(uri -> uris.add(uriToResource(uri)));

        String urisConcat = String.join(" ", uris);

        return  "VALUES " + var + " { " + urisConcat + " } ";
    }

    private String filterClause(final String id, final Set<String> input) {

        String var = toVar(id);
        Set<String> uris = new HashSet<>();
        input.forEach(uri -> uris.add(uriToResource(uri)));

        String urisConcat = String.join(" , ", uris);

        return "FILTER ( " + var + " IN ( " + urisConcat + " ) )";
    }

    private String limitOffsetClause(JsonNode jsonQuery) {
        JsonNode args = jsonQuery.get(ARGS);
        String limit = "";
        String offset = "";
        if (args != null) {
            if (args.has(LIMIT)) {
                limit = limitClause(args.get(LIMIT).asInt());
            }
            if (args.has(OFFSET)) {
                offset = offsetClause(args.get(OFFSET).asInt());
            }
        }
        return limit + offset;
    }

    private String limitClause(int limit) {
        return "LIMIT " + limit + " ";
    }

    private String offsetClause(int limit) {
        return "OFFSET " + limit + " ";
    }

    private String uriToResource(String uri) {
        return "<" + uri + ">";
    }

    private String toVar(String id) {
        return "?" + id;
    }

    private String toTriple(String subject, String predicate, String object) {
        return subject + " " + predicate + " " + object + " .";
    }

    private String langFilterClause(JsonNode field) {
        final String PATTERN = "FILTER (lang(%s) = \"%s\") . ";
        String nodeVar = toVar(field.get(NODE_ID).asText());
        JsonNode args = field.get(ARGS);
        return (args.has(LANG)) ? String.format(PATTERN, nodeVar, args.get(LANG).asText()) : "";
    }

    private String fieldPattern(String parentId, String nodeId, String predicateURI, String typeURI) {
        String predicateTriple = (parentId.equals("")) ? "" : toTriple(toVar(parentId), uriToResource(predicateURI), toVar(nodeId));
        String typeTriple = (typeURI.equals("")) ? "" : toTriple(toVar(nodeId), RDF_TYPE_URI, uriToResource(typeURI));
        return predicateTriple + typeTriple;
    }

    public String getSelectQuery(JsonNode jsonQuery, Set<String> input, String rootType) {

        Map<String, QueryFieldConfig> queryFields = schema.getQueryFields();

        Boolean root = (!jsonQuery.isArray() && queryFields.containsKey(jsonQuery.get(NAME).asText()));

        if (root) {
            if (queryFields.get(jsonQuery.get(NAME).asText()).type().equals(HGQLVocabulary.HGQL_QUERY_GET_FIELD)) {
                return getSelectRoot_GET(jsonQuery);
            } else {
                return getSelectRoot_GET_BY_ID(jsonQuery);
            }
        } else {
            return getSelectNonRoot((ArrayNode) jsonQuery, input, rootType);
        }
    }

    private String getSelectRoot_GET_BY_ID(JsonNode queryField) {

        Iterator<JsonNode> urisIter = queryField.get(ARGS).get(URIS).elements();

        Set<String> uris = new HashSet<>();

        urisIter.forEachRemaining(uri -> uris.add(uri.asText()));

        String targetName = queryField.get(TARGET_NAME).asText();
        String targetURI = schema.getTypes().get(targetName).getId();
        String graphID = ((SPARQLEndpointService) schema.getQueryFields().get(queryField.get(NAME).asText()).service()).getGraph();
        String nodeId = queryField.get(NODE_ID).asText();
        String selectTriple = toTriple(toVar(nodeId), RDF_TYPE_URI, uriToResource(targetURI));
        String valueSTR = valuesClause(nodeId, uris);
        String filterSTR = filterClause(nodeId, uris);

        JsonNode subfields = queryField.get(FIELDS);
        String subQuery = getSubQueries(subfields);

        return selectQueryClause(valueSTR + selectTriple + subQuery, graphID);
    }

    private String getSelectRoot_GET(JsonNode queryField) {

        String targetName = queryField.get(TARGET_NAME).asText();
        String targetURI = schema.getTypes().get(targetName).getId();
        String graphID = ((SPARQLEndpointService) schema.getQueryFields().get(queryField.get(NAME).asText()).service()).getGraph();
        String nodeId = queryField.get(NODE_ID).asText();
        String limitOffsetSTR = limitOffsetClause(queryField);
        String selectTriple = toTriple(toVar(nodeId), RDF_TYPE_URI, uriToResource(targetURI));
        String rootSubquery = selectSubqueryClause(nodeId, selectTriple, limitOffsetSTR);

        JsonNode subfields = queryField.get(FIELDS);
        String whereClause = getSubQueries(subfields);

        return selectQueryClause(rootSubquery + whereClause, graphID);
    }

    private String getSelectNonRoot(ArrayNode jsonQuery, Set<String> input, String rootType) {


        JsonNode firstField = jsonQuery.elements().next();
        String graphID = ((SPARQLEndpointService) schema.getTypes().get(rootType).getFields().get(firstField.get(NAME).asText()).getService()).getGraph();
        String parentId = firstField.get(PARENT_ID).asText();
        String valueSTR = valuesClause(parentId, input);

        StringBuilder whereClause = new StringBuilder();
        jsonQuery.elements().forEachRemaining(field -> whereClause.append(getFieldSubquery(field)));
        return selectQueryClause(valueSTR + (whereClause.toString()), graphID);
    }


    private String getFieldSubquery(JsonNode fieldJson) {

        String fieldName = fieldJson.get(NAME).asText();

        if (HGQLVocabulary.JSONLD.containsKey(fieldName)) {
            return "";
        }

        String fieldURI = schema.getFields().get(fieldName).getId();
        String targetName = fieldJson.get(TARGET_NAME).asText();
        String parentId = fieldJson.get(PARENT_ID).asText();
        String nodeId = fieldJson.get(NODE_ID).asText();

        String langFilter = langFilterClause(fieldJson);

        String typeURI = (schema.getTypes().containsKey(targetName)) ? schema.getTypes().get(targetName).getId() : "";

        String fieldPattern = fieldPattern(parentId, nodeId, fieldURI, typeURI);

        JsonNode subfields = fieldJson.get(FIELDS);

        String rest = getSubQueries(subfields);

        return optionalClause(fieldPattern + langFilter + rest);
    }


    private String getSubQueries(JsonNode subfields) {

        if (subfields.isNull()) {
            return "";
        }
        StringBuilder whereClause = new StringBuilder();
        subfields.elements().forEachRemaining(field -> whereClause.append(getFieldSubquery(field)));
        return whereClause.toString();
    }
}
