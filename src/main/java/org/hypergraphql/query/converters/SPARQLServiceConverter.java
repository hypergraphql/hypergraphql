package org.hypergraphql.query.converters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hypergraphql.config.schema.HGQLVocabulary;
import org.hypergraphql.config.schema.QueryFieldConfig;
import org.hypergraphql.datafetching.services.SPARQLEndpointService;
import org.hypergraphql.datamodel.HGQLSchema;

import static org.hypergraphql.util.HGQLConstants.ARGS;
import static org.hypergraphql.util.HGQLConstants.FIELDS;
import static org.hypergraphql.util.HGQLConstants.LANG;
import static org.hypergraphql.util.HGQLConstants.LIMIT;
import static org.hypergraphql.util.HGQLConstants.NAME;
import static org.hypergraphql.util.HGQLConstants.NODE_ID;
import static org.hypergraphql.util.HGQLConstants.OFFSET;
import static org.hypergraphql.util.HGQLConstants.PARENT_ID;
import static org.hypergraphql.util.HGQLConstants.TARGET_NAME;
import static org.hypergraphql.util.HGQLConstants.URIS;

@RequiredArgsConstructor
public class SPARQLServiceConverter {

    private static final String RDF_TYPE_URI = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";

    private final HGQLSchema schema;

    private String optionalClause(final String sparqlPattern) {
        return " OPTIONAL { " + sparqlPattern + " } ";
    }

    private String selectSubqueryClause(final String id,
                                        final String sparqlPattern,
                                        final String limitOffset) {
        return "{ SELECT " + toVar(id) + " WHERE { " + sparqlPattern + " } " + limitOffset + " } ";
    }

    private String selectQueryClause(final String where, final String graphID) {
        return  "SELECT * WHERE { " + graphClause(graphID, where) + " } ";
    }

    private String graphClause(final String graphID, final String where) {
        if (StringUtils.isEmpty(graphID)) {
            return where;
        } else {
            return "GRAPH <" + graphID + "> { " + where + " } ";
        }
    }

    private String valuesClause(final String id, final Collection<String> input) {
        final String var = toVar(id);
        final Collection<String> uris = new HashSet<>();
        input.forEach(uri -> uris.add(uriToResource(uri)));

        final String urisConcat = String.join(" ", uris);

        return  "VALUES " + var + " { " + urisConcat + " } ";
    }

    private String filterClause(final String id, final Collection<String> input) {

        final String var = toVar(id);
        final Collection<String> uris = new HashSet<>();
        input.forEach(uri -> uris.add(uriToResource(uri)));

        final String urisConcat = String.join(" , ", uris);

        return "FILTER ( " + var + " IN ( " + urisConcat + " ) )";
    }

    private String limitOffsetClause(final JsonNode jsonQuery) {
        final JsonNode args = jsonQuery.get(ARGS);
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

    private String limitClause(final int limit) {
        return "LIMIT " + limit + " ";
    }

    private String offsetClause(final int limit) {
        return "OFFSET " + limit + " ";
    }

    private String uriToResource(final String uri) {
        return "<" + uri + ">";
    }

    private String toVar(final String id) {
        return "?" + id;
    }

    private String toTriple(final String subject,
                            final String predicate,
                            final String object) {
        return subject + " " + predicate + " " + object + " .";
    }

    private String langFilterClause(final JsonNode field) {
        final String pattern = "FILTER (lang(%s) = \"%s\") . ";
        final String nodeVar = toVar(field.get(NODE_ID).asText());
        final JsonNode args = field.get(ARGS);
        return (args.has(LANG)) ? String.format(pattern, nodeVar, args.get(LANG).asText()) : "";
    }

    private String fieldPattern(final String parentId,
                                final String nodeId,
                                final String predicateURI,
                                final String typeURI) {
        final String predicateTriple = ("".equals(parentId)) ? "" : toTriple(toVar(parentId), uriToResource(predicateURI), toVar(nodeId));
        final String typeTriple = ("".equals(typeURI)) ? "" : toTriple(toVar(nodeId), RDF_TYPE_URI, uriToResource(typeURI));
        return predicateTriple + typeTriple;
    }

    public String getSelectQuery(final JsonNode jsonQuery,
                                 final Collection<String> input,
                                 final String rootType) {

        final Map<String, QueryFieldConfig> queryFields = schema.getQueryFields();

        final boolean root = !jsonQuery.isArray() && queryFields.containsKey(jsonQuery.get(NAME).asText());

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

    private String getSelectRoot_GET_BY_ID(final JsonNode queryField) {

        final Iterator<JsonNode> urisIter = queryField.get(ARGS).get(URIS).elements();

        final Collection<String> uris = new HashSet<>();

        urisIter.forEachRemaining(uri -> uris.add(uri.asText()));

        final String targetName = queryField.get(TARGET_NAME).asText();
        final String targetURI = schema.getTypes().get(targetName).getId();
        final String graphID = ((SPARQLEndpointService) schema.getQueryFields().get(queryField.get(NAME).asText()).service()).getGraph();
        final String nodeId = queryField.get(NODE_ID).asText();
        final String selectTriple = toTriple(toVar(nodeId), RDF_TYPE_URI, uriToResource(targetURI));
        String valueSTR = valuesClause(nodeId, uris);
        String filterSTR = filterClause(nodeId, uris);

        final JsonNode subfields = queryField.get(FIELDS);
        final String subQuery = getSubQueries(subfields);

        return selectQueryClause(valueSTR + selectTriple + subQuery, graphID);
    }

    private String getSelectRoot_GET(final JsonNode queryField) {

        final String targetName = queryField.get(TARGET_NAME).asText();
        final String targetURI = schema.getTypes().get(targetName).getId();
        final String graphID = ((SPARQLEndpointService) schema.getQueryFields().get(queryField.get(NAME).asText()).service()).getGraph();
        final String nodeId = queryField.get(NODE_ID).asText();
        final String limitOffsetSTR = limitOffsetClause(queryField);
        final String selectTriple = toTriple(toVar(nodeId), RDF_TYPE_URI, uriToResource(targetURI));
        final String rootSubquery = selectSubqueryClause(nodeId, selectTriple, limitOffsetSTR);

        final JsonNode subfields = queryField.get(FIELDS);
        final String whereClause = getSubQueries(subfields);

        return selectQueryClause(rootSubquery + whereClause, graphID);
    }

    private String getSelectNonRoot(final ArrayNode jsonQuery,
                                    final Collection<String> input,
                                    final String rootType) {


        final JsonNode firstField = jsonQuery.elements().next();
        final String graphID = ((SPARQLEndpointService) schema.getTypes().get(rootType).getFields().get(firstField.get(NAME).asText()).getService()).getGraph();
        final String parentId = firstField.get(PARENT_ID).asText();
        final String valueSTR = valuesClause(parentId, input);

        final StringBuilder whereClause = new StringBuilder();
        jsonQuery.elements().forEachRemaining(field -> whereClause.append(getFieldSubquery(field)));
        return selectQueryClause(valueSTR + (whereClause.toString()), graphID);
    }

    private String getFieldSubquery(final JsonNode fieldJson) {

        final String fieldName = fieldJson.get(NAME).asText();

        if (HGQLVocabulary.JSONLD.containsKey(fieldName)) {
            return "";
        }

        final String fieldURI = schema.getFields().get(fieldName).getId();
        final String targetName = fieldJson.get(TARGET_NAME).asText();
        final String parentId = fieldJson.get(PARENT_ID).asText();
        final String nodeId = fieldJson.get(NODE_ID).asText();

        final String langFilter = langFilterClause(fieldJson);

        final String typeURI = (schema.getTypes().containsKey(targetName)) ? schema.getTypes().get(targetName).getId() : "";

        final String fieldPattern = fieldPattern(parentId, nodeId, fieldURI, typeURI);

        final JsonNode subfields = fieldJson.get(FIELDS);

        final String rest = getSubQueries(subfields);

        return optionalClause(fieldPattern + langFilter + rest);
    }

    private String getSubQueries(final JsonNode subfields) {

        if (subfields.isNull()) {
            return "";
        }
        final StringBuilder whereClause = new StringBuilder();
        subfields.elements().forEachRemaining(field -> whereClause.append(getFieldSubquery(field)));
        return whereClause.toString();
    }
}
