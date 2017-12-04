package org.hypergraphql.query.converters;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.log4j.Logger;
import org.hypergraphql.config.system.HGQLConfig;


import java.util.*;

/**
 * Created by szymon on 15/09/2017.
 * <p>
 * This class contains jsonRewrite methods between different query/response formats
 */

public class Converter {
//    static Logger logger = Logger.getLogger(Converter.class);
//
//    private HGQLConfig config;
//    private Map<String, String> JSONLD_VOC = new HashMap<String, String>() {{
//        put("_context", "@context");
//        put("_id", "@id");
//        put("_value", "@value");
//        put("_type", "@type");
//        put("_language", "@language");
//        put("_graph", "@graph");
//    }}; //from graphqlConfig names to jsonld reserved names
//
//    private LinkedList<JsonNode> queue = new LinkedList<>();
//
//
//    public Converter() {
//        this.config = HGQLConfig.getInstance();
//    }
//
//    private final String RDF_TYPE_URI = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
//    private final String HGQL_TYPE_URI = "<http://hypergraphql/type>";
//
//    private String graphSTR(String graph, String triple) {
//        final String PATTERN = "GRAPH <%s> { %s } ";
//        return (graph.equals("")) ? triple : String.format(PATTERN, graph, triple);
//    }
//
//    private String optionalSTR(String triple, String sparqlPattern) {
//        final String PATTERN = " OPTIONAL { %s %s } ";
//        return String.format(PATTERN, triple, sparqlPattern);
//    }
//
//    private String selectSTR(String id, String sparqlPattern, String limit, String offset) {
//        final String PATTERN = "{ SELECT " + varSTR(id) + " WHERE { %s } %s %s } ";
//        return String.format(PATTERN, sparqlPattern, limit, offset);
//    }
//
//    private String limitSTR(int no) {
//        final String PATTERN = "LIMIT %s ";
//        return String.format(PATTERN, no);
//    }
//
//    private String offsetSTR(int no) {
//        final String PATTERN = "OFFSET %s ";
//        return String.format(PATTERN, no);
//    }
//
//    private String uriSTR(String uri) {
//        final String PATTERN = "<%s>";
//        return String.format(PATTERN, uri);
//    }
//
//    private String varSTR(String id) {
//        final String PATTERN = "?%s";
//        return String.format(PATTERN, id);
//    }
//
//    private String tripleSTR(String subject, String predicate, String object) {
//        final String PATTERN = "%s %s %s . ";
//        return String.format(PATTERN, subject, predicate, object);
//    }
//
//    private String langFilterSTR(JsonNode field) {
//        final String PATTERN = "FILTER (lang(%s) = \"%s\") . ";
//        String nodeVar = varSTR(field.get("nodeId").asText());
//        JsonNode args = field.get("args");
//        String langPattern = (args.has("lang")) ? String.format(PATTERN, nodeVar, args.get("lang").asText()) : "";
//        return langPattern;
//    }
//
//    private String constructSTR(String constructPattern, String wherePattern) {
//        final String PATTERN = "CONSTRUCT { %s } WHERE { %s }";
//        return String.format(PATTERN, constructPattern, wherePattern);
//    }
//
//    private String markTripleSTR(String id) {
//        String markTriple = (id.equals("")) ? "" : tripleSTR(varSTR(id), HGQL_TYPE_URI, "<http://hypergraphql/node/" + id + ">");
//        return markTriple;
//    }
//
//    private String rootTripleSTR(String id, String root) {
//        return tripleSTR(varSTR(id), HGQL_TYPE_URI, "<http://hypergraphql/query/" + root + ">");
//    }
//
//    private String fieldPattern(String parentId, String nodeId, String predicateURI, String typeURI) {
//        String predicateTriple = (parentId.equals("")) ? "" : tripleSTR(varSTR(parentId), uriSTR(predicateURI), varSTR(nodeId));
//        String typeTriple = (typeURI.equals("")) ? "" : tripleSTR(varSTR(nodeId), RDF_TYPE_URI, uriSTR(typeURI));
//        return predicateTriple + typeTriple;
//    }
//
//    private String fieldPattern(JsonNode field) {
//        String nodeId = field.get("nodeId").asText();
//        String parentId = (field.has("parentId")) ? field.get("parentId").asText() : "";
//        String predicateURI = (field.has("uri")) ? field.get("uri").asText() : "";
//        String typeURI = (field.has("targetURI")) ? field.get("targetURI").asText() : "";
//        return fieldPattern(parentId, nodeId, predicateURI, typeURI);
//    }
//
//    private String graphPattern(String fieldPattern, JsonNode field, JsonNode parentField) {
//        JsonNode args = field.get("args");
//        String graphName = (args.has("graph")) ? args.get("graph").asText() : field.get("graph").asText();
//
//        String graphPattern;
//        if (parentField != null) {
//            JsonNode parentArgs = parentField.get("args");
//            String parentGraphName = (parentArgs.has("graph")) ? parentArgs.get("graph").asText() : parentField.get("graph").asText();
//            graphPattern = (parentGraphName.equals(graphName)) ? fieldPattern : graphSTR(graphName, fieldPattern);
//        } else {
//            graphPattern = graphSTR(graphName, fieldPattern);
//        }
//        return graphPattern;
//    }
    

//    public List<Map<String, String>> graphql2sparql(JsonNode jsonQuery) {
//
//        List<Map<String, String>> output = new ArrayList<>();
//
//        jsonQuery.fieldNames().forEachRemaining(service ->
//                jsonQuery.get(service).elements().forEachRemaining(query -> {
//
//                    ObjectMapper mapper = new ObjectMapper();
//                    ObjectNode topQuery = mapper.createObjectNode();
//                    ArrayNode array = mapper.createArrayNode();
//                    array.add(query);
//                    topQuery.put(service, array);
//                    Map<String, String> queryRequest = getConstructQuery(topQuery, true);
//                    output.add(queryRequest);
//                })
//        );
//
//        while (queue.size() > 0) {
//
//            JsonNode nextQuery = queue.getFirst();
//
//            queue.removeFirst();
//
//            try {
//                Map<String, String> queryRequest = getConstructQuery(nextQuery, false);
//                output.add(queryRequest);
//            } catch (Exception e) {
//                logger.error(e);
//            }
//        }
//
//        return output;
//    }
//
//    private Map<String, String> getConstructQuery(JsonNode query, Boolean rootQuery) {
//
//        Map<String, String> output = new HashMap<>();
//        String service = query.fieldNames().next();
//        output.put("service", service);
//
//        Iterator<JsonNode> fields = query.get(service).elements();
//
//        String constructPattern = "";
//        String wherePattern = "";
//
//        String matchParent;
//
//        while (fields.hasNext()) {
//            JsonNode field = fields.next();
//
//            JsonNode args = field.get("args");
//            String nodeId = field.get("nodeId").asText();
//            String parentId = (field.has("parentId")) ? field.get("parentId").asText() : "";
//            String fieldPattern = fieldPattern(field);
//
//            matchParent = markTripleSTR(parentId);
//            String queryName = (field.has("alias")) ? field.get("alias").asText() : field.get("name").asText();
//            String rootTriple = (rootQuery) ? rootTripleSTR(nodeId, queryName) : "";
//            String markTriple = (field.has("fields")) ? markTripleSTR(nodeId) : "";
//
//            String limit = (args.has("limit")) ? limitSTR(args.get("limit").asInt()) : "";
//            String offset = (args.has("offset")) ? offsetSTR(args.get("offset").asInt()) : "";
//
//            Map<String, String> subqueries = getSubqueries(service, field);
//
//            String subConstruct = (subqueries.containsKey("construct")) ? subqueries.get("construct") : "";
//            String subWhere = (subqueries.containsKey("where")) ? subqueries.get("where") : "";
//
//            String matchPattern = (rootQuery) ? selectSTR(nodeId, fieldPattern, limit, offset) + subWhere + wherePattern : fieldPattern + subWhere + wherePattern;
//
//            constructPattern = fieldPattern + rootTriple + markTriple + subConstruct + constructPattern;
//            wherePattern = graphPattern(matchPattern, field, null);
//
//            output.put("match", matchParent);
//            output.put("var", varSTR(parentId));
//        }
//
//        wherePattern = (rootQuery) ? wherePattern : "%s " + wherePattern;
//        // String servicePattern = serviceSTR(service, wherePattern);
//        String constructQuery = constructSTR(constructPattern, wherePattern);
//
//        output.put("query", constructQuery);
//
//        return output;
//    }
//
//
//    private Map<String, String> getSubqueries(String service, JsonNode query) {
//
//        Map<String, String> output = new HashMap<>();
//
//        if (query.has("fields")) {
//            Iterator<String> serviceNames = query.get("fields").fieldNames();
//
//            while (serviceNames.hasNext()) {
//
//                String serviceName = serviceNames.next();
//
//                if (serviceName.equals(service)) {
//
//                    Iterator<JsonNode> fields = query.get("fields").get(serviceName).elements();
//
//                    String constructPattern = "";
//                    String wherePattern = "";
//
//                    while (fields.hasNext()) {
//                        Map<String, String> fieldPatterns = getSubquery(service, query, fields.next());
//                        constructPattern = constructPattern + fieldPatterns.get("construct");
//                        wherePattern = wherePattern + fieldPatterns.get("where");
//                    }
//
//                    output.put("construct", constructPattern);
//                    output.put("where", wherePattern);
//
//                } else {
//
//                    ObjectMapper mapper = new ObjectMapper();
//                    ObjectNode queueQuery = mapper.createObjectNode();
//                    queueQuery.put(serviceName, query.get("fields").get(serviceName));
//                    queue.add(queueQuery);
//                }
//            }
//        }
//
//        return output;
//    }
//
//
//    private Map<String, String> getSubquery(String service, JsonNode parentField, JsonNode field) {
//        Map<String, String> output = new HashMap<>();
//
//        String langFilter = langFilterSTR(field);
//        String fieldPattern = fieldPattern(field);
//        String nodeId = field.get("nodeId").asText();
//        String markTriple = (field.has("fields")) ? markTripleSTR(nodeId) : "";
//
//        Map<String, String> subqueries = getSubqueries(service, field);
//
//        String subConstruct = (subqueries.containsKey("construct")) ? subqueries.get("construct") : "";
//        String subWhere = (subqueries.containsKey("where")) ? subqueries.get("where") : "";
//
//        String constructPattern = fieldPattern + markTriple + subConstruct;
//        String wherePattern = graphPattern(optionalSTR(fieldPattern + langFilter, subWhere), field, parentField);
//
//        output.put("construct", constructPattern);
//        output.put("where", wherePattern);
//
//        return output;
//    }
//
//    public Map<String, Object> query2json(String query) {
//
//        HashMap<String, Object> result = new HashMap<>();
//        List<ValidationError> validationErrors = new ArrayList<>();
//
//        result.put("query", (new ObjectMapper()).createArrayNode());
//        result.put("errors", validationErrors);
//
//
//        Validator validator = new Validator();
//        Parser parser = new Parser();
//        Document document;
//
//        try {
//            document = parser.parseDocument(query);
//        } catch (Exception e) {
//            ValidationError err = new ValidationError(ValidationErrorType.InvalidSyntax, new SourceLocation(0, 0), "Invalid query syntax.");
//            validationErrors.add(err);
//            return result;
//        }
//
//        validationErrors.addAll(validator.validateDocument(config.schema(), document));
//        if (validationErrors.size() > 0) {
//            return result;
//        }
//
//        OperationDefinition opDef = (OperationDefinition) document.getDefinitions().get(0);
//
//        Map<String, Object> conversionResult = getSelectionJson(opDef.getSelectionSet());
//
//        JsonNode topQueries = (ArrayNode) conversionResult.get("query");
//        Map<String, String> context = (Map<String, String>) conversionResult.get("context");
//        result.put("query", topQueries);
//        result.put("context", context);
//
//        return result;
//    }
//
//    private Map<String, Object> getSelectionJson(SelectionSet selectionSet) {
//
//        Map<String, Object> result = new HashMap<>();
//
//        if (selectionSet == null) {
//
//            return result;
//
//        }
//
//        Map<String, String> context = new HashMap<>();
//
//        ObjectMapper mapper = new ObjectMapper();
//
//        ArrayNode fieldsJson = mapper.createArrayNode();
//
//        List<Selection> children = selectionSet.getSelections();
//
//        for (Selection child : children) {
//            if (child.getClass().getSimpleName().equals("Field")) {
//
//                Field inner = (Field) child;
//
//                Map<String, Object> childrenFields = getSelectionJson(inner.getSelectionSet());
//
//                if (childrenFields.containsKey("context")) {
//
//                    context.putAll((Map<String, String>) childrenFields.get("context"));
//
//                }
//
//                ObjectNode fieldJson = mapper.createObjectNode();
//
//                List<Argument> args = inner.getArguments();
//
//                ObjectNode argsJson = mapper.createObjectNode();
//
//                for (Argument arg : args) {
//
//                    Value val = arg.getValue();
//                    String type = val.getClass().getSimpleName();
//
//                    switch (type) {
//                        case "IntValue": {
//                            long value = ((IntValue) val).getValue().longValueExact();
//                            argsJson.put(arg.getName().toString(), value);
//                            break;
//                        }
//                        case "StringValue": {
//                            String value = ((StringValue) val).getValue().toString();
//                            argsJson.put(arg.getName().toString(), value);
//                            break;
//                        }
//                        case "BooleanValue": {
//                            Boolean value = ((BooleanValue) val).isValue();
//                            argsJson.put(arg.getName().toString(), value);
//                            break;
//                        }
//                    }
//                }
//
//                fieldJson.put("name", inner.getName());
//                if (inner.getAlias() != null) {
//                    fieldJson.put("alias", inner.getAlias());
//                }
//                fieldJson.put("args", argsJson);
//                if (childrenFields.containsKey("query")) {
//
//                    fieldJson.put("fields", (ArrayNode) childrenFields.get("query"));
//
//                }
//
//                String contextName = (inner.getAlias() != null) ? inner.getAlias() : inner.getName();
//
//                if (config.schemElementConfigMap().containsKey(contextName)) {
//                    context.put(contextName, config.schemElementConfigMap().get(contextName).id());
//                } else {
//                    if (JSONLD_VOC.containsKey(contextName)) {
//                        context.put(contextName, JSONLD_VOC.get(contextName).toString());
//                    } else {
//                        context.put(contextName, "http://hypergraphql/query/" + contextName);
//                    }
//                }
//
//                fieldsJson.add(fieldJson);
//
//            }
//        }
//        result.put("query", fieldsJson);
//        result.put("context", context);
//
//        return result;
//
//    }
//
//
//    public JsonNode includeContextInQuery(JsonNode object) {
//
//        JsonNode queryType = config.mapping().get("Query");
//
//        JsonNode result = addContext(object.deepCopy(), queryType, null);
//
//        return result;
//    }
//
//    private JsonNode addContext(JsonNode object, JsonNode type, String parentId) {
//
//        ObjectMapper mapper = new ObjectMapper();
//
//        ObjectNode result = mapper.createObjectNode();
//
//        Iterator<JsonNode> fields = object.elements();
//
//        int i = 0;
//
//        while (fields.hasNext()) {
//
//            ObjectNode subquery = (ObjectNode) fields.next();
//
//            if (parentId != null) {
//                subquery.put("parentId", parentId);
//            }
//
//            i++;
//            String nodeId = (parentId == null) ? "x" + "_" + i : parentId + "_" + i;
//
//            subquery.put("nodeId", nodeId);
//
//            String name = subquery.get("name").asText();
//            String targetName = (type.get("fields").has(name)) ? type.get("fields").get(name).get("targetName").asText() : null;
//
//            if (subquery.has("fields")) {
//                JsonNode revisedFields = addContext(subquery.get("fields"), config.mapping().get(targetName), nodeId);
//                if (revisedFields.size() > 0) {
//                    subquery.put("fields", revisedFields);
//                }
//            }
//
//            if (config.schemElementConfigMap().containsKey(targetName)) {
//                subquery.put("targetURI", config.schemElementConfigMap().get(targetName).id());
//            }
//
//            if (config.schemElementConfigMap().containsKey(name)) {
//                subquery.put("uri", config.schemElementConfigMap().get(name).id());
//            }
//
//
////            if (!JSONLD_VOC.containsKey(name)) {
////
////          //      Service service = (parentId==null) ? config.schemElementConfigMap().get(name).service(): config.schemElementConfigMap().get(name).service();
////                Service service = (parentId==null) ? config.queryFields().get(name).service(): config.fields().get(name).service();
////
////                subquery.put("graph", service.getGraph());
////                String endpoint = service.getId();
////                ArrayNode subfields = (result.has(endpoint)) ? (ArrayNode) result.get(endpoint) : mapper.createArrayNode();
////                subfields.add(subquery);
////                if (subfields.size() > 0) {
////                    result.put(service.getId(), subfields);
////                }
//            //}
//
//        }
//
//        return result;
//    }

}

