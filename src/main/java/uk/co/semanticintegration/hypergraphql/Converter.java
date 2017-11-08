package uk.co.semanticintegration.hypergraphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.language.TypeDefinition;
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

    private LinkedList<QueryInQueue> queryQueue = new LinkedList<>();


    public Converter(Config config) {
        this.config = config;
    }

    private class QueryInQueue {
        JsonNode query;
        String rootNode;
        String childNode;

        QueryInQueue(JsonNode query) {
            this.query = query;
            this.childNode = "x";
        }

        QueryInQueue(JsonNode query, String rootNode, String childNode) {
            this.query = query;
            this.rootNode = rootNode;
            this.childNode = childNode;
        }
    }


    public Map<String,Object> jsonLDdata(Map<String, Object> data, JsonNode jsonQuery) throws IOException {

        Map<String, Object> ldContext = new HashMap<>();
        Map<String, Object> output = new HashMap<>();

        jsonQuery.elements().forEachRemaining(elem ->
                ldContext.put(elem.get("name").asText(), "http://hypergraphql/" + elem.get("name").asText()));

        Pattern namePtrn = Pattern.compile("\"name\":\"([^\"]*)\"");
        Matcher nameMtchr = namePtrn.matcher(jsonQuery.toString());

        while(nameMtchr.find())
        {
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

    public List<String> graphql2sparql(JsonNode jsonQuery) {

        for (JsonNode topQuery : jsonQuery) {
            QueryInQueue root = new QueryInQueue(topQuery);
            queryQueue.addLast(root);
        }

        List<String> output = new ArrayList<>();

        while (queryQueue.size() > 0) {

            QueryInQueue nextQuery = queryQueue.getFirst();
            queryQueue.removeFirst();

            try {
                String constructQuery = getConstructQuery(nextQuery);
                output.add(constructQuery);
            } catch (Exception e) {
                logger.error(e);
            }
        }

        return output;
    }

    public String getConstructQuery(QueryInQueue jsonQuery) {

        //this method will convert a given graphql query field into a SPARQL construct query
        // that will retrieve all relevant data in one go and put it into an in-memory jena store

        JsonNode root = jsonQuery.query;
        JsonNode args = root.get("args");
        String fieldName = root.get("name").asText();
        String graphId = (args.has("graph")) ? args.get("graph").asText() : config.predicateGraph(fieldName);
        String endpointId = (args.has("endpoint")) ? args.get("endpoint").asText() : config.predicateEndpoint(fieldName);

        String uri_ref = String.format("<%s>", config.predicateURI(fieldName));
        String parentNode = jsonQuery.rootNode;
        String selfNode = jsonQuery.childNode;
        String selfVar = "?" + selfNode;
        String parentVar = "?" + parentNode;
        String constructedTriple;
        String selectedTriple;
        String rootMatch = "";
        String nodeMark = "";
        String innerPattern;

        if (parentNode == null) {
            String limitParams = "";
            if (args.has("limit")) {
                limitParams = "LIMIT " + args.get("limit");
                if (args.has("offset")) {
                    limitParams = limitParams + " OFFSET " + args.get("offset");
                }
            }
            nodeMark = String.format("?%1$s <http://hgql/root> <http://hgql/node_%1$s> . ", selfNode );
            constructedTriple = String.format(" %1$s <http://hgql/root> %2$s . ", selfVar, uri_ref);
            selectedTriple = String.format(" %1$s a %2$s . ", selfVar, uri_ref);
            innerPattern = String.format(" { SELECT %s WHERE { %s } %s } ", selfVar, selectedTriple, limitParams);
        } else {
            rootMatch = String.format(" %s <http://hgql/root> <http://hgql/node_%s> . ", parentVar, parentNode);
            constructedTriple = String.format(" %s %s %s . ", parentVar, uri_ref, selfVar);
            innerPattern = constructedTriple;
        }

        String topConstructTemplate = "CONSTRUCT { " + nodeMark + " %1$s } WHERE { " + rootMatch + " SERVICE <%2$s> { %3$s } } ";

        String[] triplePatterns = getSubquery(root, selfNode, graphId, endpointId);

        constructedTriple = constructedTriple + triplePatterns[0];
        innerPattern = innerPattern + triplePatterns[1];
        String graphPattern = "GRAPH <%1$s> { %2$s }";

        if (!graphId.isEmpty()) {
            innerPattern = String.format(graphPattern, graphId, innerPattern);
        }

        return String.format(topConstructTemplate,
                constructedTriple,
                endpointId,
                innerPattern);
    }

    private String[] getSubquery(JsonNode node, String parentNode, String parentGraphId, String parentEndpointId) {

        String parentVar = "?" + parentNode;
        String constructPattern = "%1$s <%2$s> %3$s . ";
        String optionalPattern = "OPTIONAL { %1$s } ";
        String triplePattern = "%1$s <%2$s> %3$s %4$s. %5$s";
        String graphPattern = "GRAPH <%1$s> { %2$s }";

        String[] output = new String[2];

        JsonNode fields = node.get("fields");

        if (fields == null) {
            output[0] = "";
            output[1] = "";
        } else {

            int n = 0;

            List<String> childConstruct = new ArrayList<>();
            List<String> childOptional = new ArrayList<>();

            for (JsonNode field : fields) {

                if (!JSONLD_VOC.containsKey(field.get("name").asText())) {

                    JsonNode args = field.get("args");

                    String fieldName = field.get("name").asText();
                    String graphId = (args.has("graph")) ? args.get("graph").asText() : config.predicateGraph(fieldName);
                    String endpointId = (args.has("endpoint")) ? args.get("endpoint").asText() : config.predicateEndpoint(fieldName);

                    n++;

                    String childNode = parentNode + "_" + n;
                    String childVar = "?" + childNode;

                    if (!endpointId.equals(parentEndpointId)) {
                        QueryInQueue newQuery = new QueryInQueue(field, parentNode, childNode);
                        queryQueue.addLast(newQuery);

                        String nodeMark = String.format("?%1$s <http://hgql/root> <http://hgql/node_%1$s> .", parentNode);

                        childConstruct.add(nodeMark);

                    } else {

                        String[] grandChildPatterns = getSubquery(field, childNode, graphId, endpointId);

                        String childConstructPattern = String.format(constructPattern,
                                parentVar,
                                config.predicateURI(fieldName),
                                childVar
                        );

                        String langFilter = "";

                        if (args.has("lang"))
                            langFilter = "FILTER (lang(" + childVar + ")=" + "\"" + args.get("lang").asText() + "\") ";

                        String childOptionalPattern = String.format(triplePattern,
                                parentVar,
                                config.predicateURI(fieldName),
                                childVar,
                                grandChildPatterns[1],
                                langFilter
                        );

                        if (!graphId.equals(""))
                            if (!graphId.equals(parentGraphId) || !endpointId.equals(parentEndpointId)) {
                                childOptionalPattern = String.format(graphPattern, graphId, childOptionalPattern);
                            }

                        childOptionalPattern = String.format(optionalPattern, childOptionalPattern);

                        childConstruct.add(childConstructPattern);
                        childConstruct.add(grandChildPatterns[0]);

                        childOptional.add(childOptionalPattern);
                    }
                }
            }

            output[0] = String.join(" ", childConstruct);
            output[1] = String.join(" ", childOptional);

        }
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

         //   System.out.println(object.toString());

            logger.debug("Generated query JSON: " + object.toString()); //debug message

            return object;


     //       TypeDefinition queryType = config.schema().types().get("Query");
      //      JsonNode result = includeTypes(object, queryType);

        //    return result;
        } catch (IOException e) {

            logger.error(e);

            return null;
        }
    }

    private JsonNode includeTypes(JsonNode object, TypeDefinition type) {

        JsonNode result = object;

        object.elements().forEachRemaining(subquery -> {
            // System.out.println(subquery.get("name"));

            ObjectMapper mappers = new ObjectMapper();
            try {
                JsonNode resultJson = mappers.readTree(new ObjectMapper().writeValueAsString(type));
               // System.out.println(resultJson.toString());
                resultJson.get("fieldDefinitions").elements().forEachRemaining(el ->
                {
                    if (el.get("name").equals(subquery.get("name"))) {
                     //   System.out.println(el.toString());
                     //   System.out.println(el.get("type").get("type").get("name").asText());
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }

        });

        return result;
    }

}
