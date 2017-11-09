package uk.co.semanticintegration.hypergraphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by szymon on 15/09/2017.
 * <p>
 * This class contains jsonRewrite methods between different query/response formats
 */

public class Converter {
    static Logger logger = Logger.getLogger(Converter.class);

    private Config config;
    private Map<String, String> JSONLD_VOC = new HashMap<String, String>() {{
        put("_context", "@context");
        put("_id", "@id");
        put("_value", "@value");
        put("_type", "@type");
        put("_language", "@language");
        put("_graph", "@graph");
    }}; //from graphql names to jsonld reserved names

    private LinkedList<JsonNode> queue = new LinkedList<>();


    public Converter(Config config) {
        this.config = config;
    }

    private final String RDF_TYPE_URI = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
    private final String HGQL_TYPE_URI = "<http://hypergraphql/type>";

    private String serviceSTR(String service, String sparqlPattern) {
        final String PATTERN = "SERVICE <%s> { %s }";
        return String.format(PATTERN, service, sparqlPattern);
    }

    private String graphSTR(String graph, String triple) {
        final String PATTERN = "{ GRAPH <%s> { %s } }";
        return (graph.equals("")) ? triple : String.format(PATTERN, graph, triple);
    }

    private String optionalSTR(String triple, String sparqlPattern) {
        final String PATTERN = " OPTIONAL { %s %s } ";
        return String.format(PATTERN, triple, sparqlPattern);
    }

    private String selectSTR(String id, String sparqlPattern, String limit, String offset) {
        final String PATTERN = "{ SELECT " + varSTR(id) + " WHERE { %s } %s %s }";
        return String.format(PATTERN, sparqlPattern, limit, offset);
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

    private String markTripleSTR(String id) {
        String markTriple = (id.equals("")) ? "" : tripleSTR(varSTR(id), HGQL_TYPE_URI, "<http://hypergraphql/node_" + id + ">");
        return markTriple;
    }

    private String rootTripleSTR(String id, String root) {
        return tripleSTR(varSTR(id), HGQL_TYPE_URI, "<http://hypergraphql/" + root + ">");
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

    private String graphPattern(String fieldPattern, JsonNode field) {
        JsonNode args = field.get("args");
        String graphName = (args.has("graph")) ? args.get("graph").asText() : field.get("graph").asText();
        String graphPattern = graphSTR(graphName, fieldPattern);
        return graphPattern;
    }


    public List<String> graphql2sparql(JsonNode jsonQuery) {

        List<String> output = new ArrayList<>();

        jsonQuery.fieldNames().forEachRemaining(service ->
                jsonQuery.get(service).elements().forEachRemaining(query -> {

                    ObjectMapper mapper = new ObjectMapper();
                    ObjectNode topQuery = mapper.createObjectNode();
                    ArrayNode array = mapper.createArrayNode();
                    array.add(query);
                    topQuery.put(service, array);
                    String constructQuery = getConstructQuery(topQuery, true);
                    output.add(constructQuery);
                })
        );

        while (queue.size() > 0) {

            JsonNode nextQuery = queue.getFirst();
            queue.removeFirst();

            try {
                String constructQuery = getConstructQuery(nextQuery, false);
                output.add(constructQuery);
            } catch (Exception e) {
                logger.error(e);
            }
        }

        return output;
    }

    private String getConstructQuery(JsonNode query, Boolean rootQuery) {


        String service = query.fieldNames().next();

        Iterator<JsonNode> fields = query.get(service).elements();

        String constructPattern = "";
        String wherePattern = "";

        String matchParent = "";

        while (fields.hasNext()) {
            JsonNode field = fields.next();

            JsonNode args = field.get("args");
            String nodeId = field.get("nodeId").asText();
            String parentId = (field.has("parentId")) ? field.get("parentId").asText() : "";
            String fieldPattern = fieldPattern(field);

            String graphPattern = graphPattern(fieldPattern, field);
            matchParent = markTripleSTR(parentId);
            String rootTriple = (rootQuery) ? rootTripleSTR(nodeId, field.get("name").asText()) : "";
            String markTriple = markTripleSTR(nodeId);

            String limit = (args.has("limit")) ? limitSTR(args.get("limit").asInt()) : "";
            String offset = (args.has("offset")) ? offsetSTR(args.get("offset").asInt()) : "";

            String matchPattern = (rootQuery) ? selectSTR(nodeId, graphPattern, limit, offset) : graphPattern;

            Map<String, String> subqueries = getSubqueries(service, field);

            String subConstruct = (subqueries.containsKey("construct")) ? subqueries.get("construct") : "";
            String subWhere = (subqueries.containsKey("where")) ? subqueries.get("where") : "";

            wherePattern = matchPattern + subWhere + wherePattern;
            constructPattern = fieldPattern + rootTriple + markTriple + subConstruct + constructPattern;

        }

        String servicePattern = serviceSTR(service, wherePattern);
        String constructQuery = constructSTR(constructPattern, matchParent + servicePattern);

        return constructQuery;
    }


    private Map<String, String> getSubqueries(String service, JsonNode query) {

        Map<String, String> output = new HashMap<>();

        if (query.has("fields")) {
            Iterator<String> serviceNames = query.get("fields").fieldNames();

            while (serviceNames.hasNext()) {

                String serviceName = serviceNames.next();

                if (serviceName.equals(service)) {

                    Iterator<JsonNode> fields = query.get("fields").get(serviceName).elements();

                    String constructPattern = "";
                    String wherePattern = "";

                    while (fields.hasNext()) {
                        Map<String, String> fieldPatterns = getSubquery(service, fields.next());
                        constructPattern = constructPattern + fieldPatterns.get("construct");
                        wherePattern = wherePattern + fieldPatterns.get("where");
                    }

                    output.put("construct", constructPattern);
                    output.put("where", wherePattern);

                } else {
                    ObjectMapper mapper = new ObjectMapper();
                    ObjectNode queueQuery = mapper.createObjectNode();
                    queueQuery.put(serviceName, queueQuery);
                    queue.add(queueQuery);
                }
            }
        }

        return output;
    }


    private Map<String, String> getSubquery(String service, JsonNode field) {
        Map<String, String> output = new HashMap<>();

        String langFilter = langFilterSTR(field);
        String fieldPattern = fieldPattern(field);
        String nodeId = field.get("nodeId").asText();
        String markTriple = (field.has("fields")) ? markTripleSTR(nodeId) : "";
        String graphPattern = graphPattern(fieldPattern + langFilter, field);

        Map<String, String> subqueries = getSubqueries(service, field);

        String subConstruct = (subqueries.containsKey("construct")) ? subqueries.get("construct") : "";
        String subWhere = (subqueries.containsKey("where")) ? subqueries.get("where") : "";

        String constructPattern = fieldPattern + markTriple + subConstruct;
        String wherePattern = optionalSTR(graphPattern, subWhere);

        output.put("construct", constructPattern);
        output.put("where", wherePattern);

        return output;
    }

    public JsonNode query2json(String query) {

        query = query
                .replaceAll(",", " ")
                .replaceAll("\\s*:\\s*", ":")
                .replaceAll(",", " ")
                .replaceAll("\\{", " { ")
                .replaceAll("}", " } ")
                .replaceAll("\\(", " ( ")
                .replaceAll("\\)", " ) ")
                .replaceAll("\\s+", " ")
                .replaceAll("\\{", "<")
                .replaceAll("}", ">");

        Pattern namePtrn;
        Matcher nameMtchr;

        do {
            namePtrn = Pattern.compile("\\s(\\w+)\\s");
            nameMtchr = namePtrn.matcher(query);

            query = query.replaceAll("\\s(\\w+)\\s", " \"name\":\"$1\" ");

        } while (nameMtchr.find());

        do {
            namePtrn = Pattern.compile("\\s(\\w+):");
            nameMtchr = namePtrn.matcher(query);

            query = query.replaceAll("\\s(\\w+):", " \"$1\":");

        } while (nameMtchr.find());

        do {
            namePtrn = Pattern.compile("[^{](\"name\":\"\\w+\")(\\s(\\(\\s([^()]*)\\s\\)))?(\\s<([^<>]*)>)");
            nameMtchr = namePtrn.matcher(query);

            query = query
                    .replaceAll("(\"name\":\"\\w+\")\\s\\(\\s([^()]*)\\s\\)\\s<([^<>]*)>", "{$1, \"args\":{$2}, \"fields\":[$3]}")
                    .replaceAll("(\"name\":\"\\w+\")\\s<([^<>]*)>", "{$1, \"args\":{}, \"fields\":[$2]}");

        } while (nameMtchr.find());

        query = query
                .replaceAll("(\"name\":\"\\w+\")\\s\\(\\s([^()]*)\\s\\)", "{$1, \"args\":{$2}}")
                .replaceAll("(\"name\":\"\\w+\")\\s", "{$1, \"args\":{}} ");

        query = query
                .replaceAll("([^,])\\s\"", "$1, \"")
                .replaceAll("}\\s*\\{", "}, {")
                .replaceAll("<", "[")
                .replaceAll(">", "]");

        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode object = mapper.readTree(query);

            logger.debug("Generated query JSON: " + object.toString()); //debug message

            return object;
        } catch (IOException e) {

            logger.error(e);

            return null;
        }
    }

    public JsonNode includeContextInQuery(JsonNode object) {

        JsonNode queryType = config.mapping().get("Query");

        JsonNode result = addContext(object.deepCopy(), queryType, null);

        return result;
    }

    private JsonNode addContext(JsonNode object, JsonNode type, String parentId) {

        ObjectMapper mapper = new ObjectMapper();

        ObjectNode result = mapper.createObjectNode();

        Iterator<JsonNode> fields = object.elements();

        int i = 0;

        while (fields.hasNext()) {

            ObjectNode subquery = (ObjectNode) fields.next();

            if (parentId != null) {
                subquery.put("parentId", parentId);
            }

            i++;
            String nodeId = (parentId == null) ? "x" + "_" + i : parentId + "_" + i;

            subquery.put("nodeId", nodeId);

            String name = subquery.get("name").asText();
            String targetName = (type.get("fields").has(name)) ? type.get("fields").get(name).get("targetName").asText() : null;

            if (subquery.has("fields")) {
                JsonNode revisedFields = addContext(subquery.get("fields"), config.mapping().get(targetName), nodeId);
                if (revisedFields.size() > 0) {
                    subquery.put("fields", revisedFields);
                }
            }

            if (config.containsPredicate(targetName)) {
                subquery.put("targetURI", config.predicateURI(targetName));
            }

            if (config.containsPredicate(name)) {
                subquery.put("uri", config.predicateURI(name));
            }

            if (!JSONLD_VOC.containsKey(name)) {
                subquery.put("graph", config.predicateGraph(name));
                String endpoint = config.predicateEndpoint(name);
                ArrayNode subfields = (result.has(endpoint)) ? (ArrayNode) result.get(endpoint) : mapper.createArrayNode();
                subfields.add(subquery);
                if (subfields.size() > 0) {
                    result.put(endpoint, subfields);
                }
            }
        }

        return result;
    }


    public Map<String, Object> jsonLDdata(Map<String, Object> data, JsonNode jsonQuery) throws IOException {

        Map<String, Object> ldContext = new HashMap<>();
        Map<String, Object> output = new HashMap<>();

        jsonQuery.elements().forEachRemaining(elem ->
                ldContext.put(elem.get("name").asText(), "http://hypergraphql/" + elem.get("name").asText())
        );

        Pattern namePtrn = Pattern.compile("\"name\":\"([^\"]*)\"");
        Matcher nameMtchr = namePtrn.matcher(jsonQuery.toString());

        while (nameMtchr.find()) {
            String find = nameMtchr.group(1);
            if (!ldContext.containsKey(find)) {
                if (JSONLD_VOC.containsKey(find)) {
                    ldContext.put(find, JSONLD_VOC.get(find));
                } else {
                    if (config.containsPredicate(find)) {
                        ldContext.put(find, config.predicateURI(find));
                    }
                }
            }
        }

        output.putAll(data);
        output.put("@context", ldContext);

        return output;
    }

}

