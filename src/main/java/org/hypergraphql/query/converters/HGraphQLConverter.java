package org.hypergraphql.query.converters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hypergraphql.config.schema.QueryFieldConfig;
import org.hypergraphql.datamodel.HGQLSchema;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_QUERY_GET_FIELD;

public class HGraphQLConverter {
    private HGQLSchema schema;


    public  HGraphQLConverter(HGQLSchema schema ) {

        this.schema = schema;
    }
    private String urisArgSTR(Set<String> uris) {

        final String QUOTE = "\"%s\"";
        final String ARG = "(uris:[%s])";

        Set<String> quotedUris = new HashSet<>();

        for (String  uri : uris) {
            quotedUris.add(String.format(QUOTE, uri));
        }

        String uriSequence = String.join(",", quotedUris);

        return String.format(ARG, uriSequence);
    }

    private String getArgsSTR(JsonNode getArgs) {

        if (getArgs!=null) return "";

        final String LIM = "limit:%s ";
        final String OFF = "offset:%s ";
        final String ARG = "(%s)";

        String argsStr = "";

        if (getArgs.has("limit")) {
            argsStr += String.format(LIM, getArgs.get("limit").asInt());
        }
        if (getArgs.has("offset")) {
            argsStr += String.format(OFF, getArgs.get("offset").asInt());
        }


        return String.format(ARG, argsStr);
    }

    private String langSTR(ObjectNode langArg) {

        if (langArg.isNull()) return "";

        final String LANGARG = "(lang:\"%s\")";

        return String.format(LANGARG, langArg.get("lang").asText());
    }

    private String querySTR(String content) {

        final String QUERY = "{ %s }";

        return String.format(QUERY, content);

    }


    public String convertToHGraphQL(JsonNode jsonQuery, Set<String> input, String rootType) {

        Map<String, QueryFieldConfig> queryFields = schema.getQueryFields();

        Boolean root = (!jsonQuery.isArray() && queryFields.containsKey(jsonQuery.get("name").asText()));

        if (root) {
            if (queryFields.get(jsonQuery.get("name").asText()).type().equals(HGQL_QUERY_GET_FIELD)) {
                return getSelectRoot_GET(jsonQuery);
            } else {
                return getSelectRoot_GET_BY_ID(jsonQuery);
            }
        } else {
            return getSelectNonRoot((ArrayNode) jsonQuery, input, rootType);

        }

    }

    private String getSelectRoot_GET_BY_ID(JsonNode jsonQuery) {

        Set<String> uris = new HashSet<>();

        ArrayNode urisArray = (ArrayNode) jsonQuery.get("args").get("uris");

        urisArray.elements().forEachRemaining(el -> uris.add(el.asText()));

        String key = jsonQuery.get("name").asText() + urisArgSTR(uris);

        String content = getSubQuery(jsonQuery.get("fields"), jsonQuery.get("targetName").asText());

        return querySTR(key + content);
    }


    private String getSelectRoot_GET(JsonNode jsonQuery) {

        String key = jsonQuery.get("name").asText() + getArgsSTR(jsonQuery.get("args"));

        String content = getSubQuery(jsonQuery.get("fields"), jsonQuery.get("targetName").asText());

        return querySTR(key + content);

    }

    private String getSelectNonRoot(ArrayNode jsonQuery, Set<String> input, String rootType) {


        String topQueryFieldName = rootType + "_GET_BY_ID";

        String key = topQueryFieldName + urisArgSTR(input);

        String content = getSubQuery(jsonQuery, rootType);

        return querySTR(key + content);

    }


    private String getSubQuery(JsonNode fieldsJson, String parentType) {

        Set<String> subQueryStrings = new HashSet<>();

        if (schema.getTypes().containsKey(parentType)) {
            subQueryStrings.add("_id");
            subQueryStrings.add("_type");
        }

        if (fieldsJson==null || fieldsJson.isNull()) {
            if (subQueryStrings.isEmpty()) {
                return "";
            } else {
                querySTR(String.join(" ", subQueryStrings));
            }
        }

        else {


            Iterator<JsonNode> fields = ((ArrayNode) fieldsJson).elements();

            fields.forEachRemaining(field -> {
                ArrayNode fieldsArray = (field.get("fields").isNull()) ? null : (ArrayNode) field.get("fields");
                String arg = (field.get("args").isNull()) ? "" : langSTR((ObjectNode) field.get("args"));
                String fieldString = field.get("name").asText() + arg + " " + getSubQuery(fieldsArray, field.get("targetName").asText());
                subQueryStrings.add(fieldString);
            });
        }

        if (!subQueryStrings.isEmpty()) {
            return querySTR(String.join(" ", subQueryStrings));
        } else {
            return "";
        }
    }

}
