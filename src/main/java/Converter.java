import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.language.TypeDefinition;
import org.apache.jena.query.QuerySolution;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by szymon on 15/09/2017.
 *
 * This class contains jsonRewrite methods between different query/response formats
 */
public class Converter {


    JsonNode globalContext;
    Map<String, String> JSONLD_VOC = new HashMap<String, String>() {{
        put("_context", "@context");
        put("_id", "@id");
        put("_value", "@value");
        put("_type", "@type");
        put("_language", "@language");
        put("_graph", "@graph");
    }}; //from graphql names to jsonld reserved names

    List<QueryInQueue> queryQueue = new ArrayList<>();


    public Converter(Config config) {
        this.globalContext = config.context;
    }

    class Traversal {
        Object data;
        Map<String, String> context;
    }

    class QueryInQueue {
        JsonNode query;
        String rootNode;
        String childNode;


        public QueryInQueue(JsonNode query, String rootNode, String childNode) {
            this.query = query;
            this.rootNode = rootNode;
            this.childNode = childNode;
        }
    }



    public List<String> graphql2sparql (String query) {

        JsonNode jsonQuery = query2json(query);

        for (JsonNode topQuery : jsonQuery) {
            QueryInQueue root = new QueryInQueue(topQuery, null, "x");
            queryQueue.add(root);
        }

        List<String> output = new ArrayList<>();

        for (QueryInQueue nextQuery : queryQueue ) {
            try {
                String constructQuery = getConstructQuery(nextQuery);
                output.add(constructQuery);
            } catch (Exception e) {
                System.out.println(e.fillInStackTrace());
            }
        }

        return output;
    }

    public String getConstructQuery(QueryInQueue jsonQuery) {


        //this method will convert a given graphql query field into a SPARQL construct query
        // that will retrieve all relevant data in one go and put it into an in-memory jena store

        JsonNode root = jsonQuery.query;
        JsonNode args = root.get("args");
        String graphName = globalContext.get("@predicates").get(root.get("name").asText()).get("@namedGraph").asText();
        String graphId;
        if (!args.has("graphName")) graphId = globalContext.get("@namedGraphs").get(graphName).get("@id").asText();
            else graphId = args.get("graphName").asText();
        String endpointName = globalContext.get("@namedGraphs").get(graphName).get("@endpoint").asText();
        String endpointId;
        if (!args.has("endpoint")) endpointId = globalContext.get("@endpoints").get(endpointName).get("@id").asText();
            else endpointId = args.get("endpoint").asText();
        String uri_ref = String.format("<%s>", globalContext.get("@predicates").get(root.get("name").asText()).get("@id").asText());
        String parentNode = jsonQuery.rootNode;
        String selfNode = jsonQuery.childNode;
        String selfVar = "?" + selfNode;
        String parentVar = "?" + parentNode;
        String constructedTriple = "";
        String rootMatch = "";
        String innerPattern;
        if (parentNode==null) {
            String limitParams = "";
            if (args.has("limit")) {
                limitParams = "LIMIT " + args.get("limit");
                if (args.has("offset")) {
                    limitParams = limitParams + " OFFSET "+ args.get("offset");
                }
            }
            constructedTriple = String.format(" ?x a %s . ", uri_ref);
            innerPattern = String.format("{ SELECT ?x WHERE { %s } %s } ", constructedTriple, limitParams);
        } else {
            rootMatch = String.format(" %s a <node_%s> . ", parentVar, parentNode);
            constructedTriple = String.format(" %s %s %s . ", parentVar, uri_ref, selfVar);
            innerPattern = constructedTriple;
        }

        String nodeMark = String.format("?%1$s a <node_%1$s> .", selfNode);

        String topConstructTemplate = "CONSTRUCT { "+nodeMark+" %1$s } WHERE { "+ rootMatch +" SERVICE <%2$s> { %3$s } } ";

        String[] triplePatterns = getSubquery(root, selfVar, graphId, endpointId);

        constructedTriple = constructedTriple + triplePatterns[0];
        innerPattern = innerPattern + triplePatterns[1];
        String graphPattern = "GRAPH <%1$s> { %2$s }";

        if (!graphId.equals("")) innerPattern = String.format(graphPattern, graphId, innerPattern);

        String constructQuery = String.format(topConstructTemplate,
                constructedTriple,
                endpointId,
                innerPattern);

        System.out.println(constructQuery);

        return constructQuery;
    }

    String[] getSubquery(JsonNode node, String parentVar, String parentGraphId, String parentEndpointId) {

        String constructPattern = "%1$s <%2$s> %3$s . ";
        String optionalPattern = "OPTIONAL { %1$s } ";
        String triplePattern = "%1$s <%2$s> %3$s %4$s. %5$s";
        String graphPattern = "GRAPH <%1$s> { %2$s }";
        String servicePattern = "SERVICE <%1$s> { %2$s }";

        String[] output = new String[2];

        JsonNode fields = node.get("fields");

        if (fields==null) {
            output[0] = "";
            output[1] = "";
        } else {

            int n = 0;

            List<String> childConstruct = new ArrayList<>();
            List<String> childOptional = new ArrayList<>();

            for (JsonNode field : fields) {

                if (!JSONLD_VOC.containsKey(field.get("name").asText())) {

                    JsonNode args = field.get("args");

                    String graphName = globalContext.get("@predicates").get(field.get("name").asText()).get("@namedGraph").asText();
                    String graphId;
                    if (!args.has("graphName")) graphId = globalContext.get("@namedGraphs").get(graphName).get("@id").asText();
                    else graphId = args.get("graphName").asText();

                    String endpointName = globalContext.get("@namedGraphs").get(graphName).get("@endpoint").asText();
                    String endpointId;
                    if (!args.has("endpoint")) endpointId = globalContext.get("@endpoints").get(endpointName).get("@id").asText();
                    else endpointId = args.get("endpoint").asText();

                    n++;

                    String childVar = parentVar + "_" + n;

                    String[] grandChildPatterns = getSubquery(field, childVar, graphId, endpointId);

                    String childConstructPattern = String.format(constructPattern,
                            parentVar,
                            globalContext.get("@predicates").get(field.get("name").asText()).get("@id").asText(),
                            childVar
                    );

                    String langFilter = "";

                    if (args.has("lang")) langFilter = "FILTER (lang("+childVar + ")="+"\""+args.get("lang").asText()+"\") ";

                    String childOptionalPattern = String.format(triplePattern,
                            parentVar,
                            globalContext.get("@predicates").get(field.get("name").asText()).get("@id").asText(),
                            childVar,
                            grandChildPatterns[1],
                            langFilter
                    );

                    if (!graphId.equals(""))
                        if (!graphId.equals(parentGraphId)||!endpointId.equals(parentEndpointId)) {
                            childOptionalPattern = String.format(graphPattern, graphId, childOptionalPattern);
                        }

                    if (!endpointId.equals(parentEndpointId)) {
                        childOptionalPattern = String.format(servicePattern, endpointId, childOptionalPattern);
                    }

                    childOptionalPattern = String.format(optionalPattern, childOptionalPattern);

                    childConstruct.add(childConstructPattern);
                    childConstruct.add(grandChildPatterns[0]);

                    childOptional.add(childOptionalPattern);
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

            query=query.replaceAll("\\s(\\w+)\\s", " \"name\":\"$1\" ");

        } while (nameMtchr.find());

        do {
            namePtrn = Pattern.compile("\\s(\\w+):");
            nameMtchr = namePtrn.matcher(query);

            query=query.replaceAll("\\s(\\w+):", " \"$1\":");

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

            System.out.println(object.toString()); //debug message

            return object;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    public Object graphql2jsonld (Map<String, Object> dataObject) {

            ArrayList<Object> graphData = new ArrayList<>();
            Map<String,String> graphContext = new HashMap<>();

            Iterator<String> queryFields = dataObject.keySet().iterator();

            while (queryFields.hasNext()) {

                String field = queryFields.next();
                String newType = globalContext.get("@predicates").get(field).get("@id").asText();

                Converter.Traversal restructured = jsonRewrite(dataObject.get(field));

                if (restructured.data.getClass() == ArrayList.class) {
                    for (Object data : (ArrayList<Object>) restructured.data) ((HashMap) data).put("@type", newType);
                    graphData.addAll((ArrayList<Object>) restructured.data);
                }
                if (restructured.data.getClass() == HashMap.class) {
                    ((HashMap) restructured.data).put("@type", newType);
                    graphData.add(restructured.data);
                }

                Set<String> newContext = restructured.context.keySet();

                for (String key : newContext) {
                    graphContext.put(key, restructured.context.get(key));
                }
            }

            Map<String, Object> output = new HashMap<>();

            output.put("@graph", graphData);
            output.put("@context", graphContext);
            return output;
        }


    public Converter.Traversal jsonRewrite(Object dataObject) {

        if (dataObject == null) {

            Converter.Traversal restructured = new Converter.Traversal();
            restructured.data = new ArrayList<>();
            restructured.context = new HashMap<>();

            return restructured;
        }

        if (dataObject.getClass()==ArrayList.class) {

            ArrayList<Object> newList = new ArrayList<>();
            Map<String, String> context = new HashMap<>();
            for (Object item : (ArrayList<Object>) dataObject) {
                Converter.Traversal itemRes = jsonRewrite(item);
                newList.add(itemRes.data);
                context.putAll(itemRes.context);
            }
            Converter.Traversal restructured = new Converter.Traversal();
            restructured.data = newList;
            restructured.context = context;

            return restructured;
        }

        if (dataObject.getClass()!=LinkedHashMap.class) {


            Converter.Traversal restructured = new Converter.Traversal();
            restructured.data = dataObject;
            restructured.context = new HashMap<>();

            return restructured;
        } else {

            Map<String, Object> mapObject = (Map<String, Object>) dataObject;
            Map<String, Object> targetObject = new HashMap<>();

            Set<String> keys = mapObject.keySet();

            Map<String, String> context = new HashMap<>();

            for (String key: keys) {
                Object child = mapObject.get(key);
                Converter.Traversal childRes = jsonRewrite(child);
                if (JSONLD_VOC.containsKey(key)) targetObject.put(JSONLD_VOC.get(key), childRes.data);
                else {
                    targetObject.put(key, childRes.data);
                    context.put(key, globalContext.get("@predicates").get(key).get("@id").asText());
                }
                context.putAll(childRes.context);
            }

            Converter.Traversal restructured = new Converter.Traversal();
            restructured.data = targetObject;
            restructured.context = context;

            return restructured;
        }

    }

    public JsonNode definitionToJson(TypeDefinition type) {

        String typeData = type.toString();
        Pattern namePtrn = Pattern.compile("(\\w+)\\{");
        Matcher nameMtchr = namePtrn.matcher(typeData);

        while (nameMtchr.find()) {
            String find = nameMtchr.group(1);
            typeData = typeData.replace(find + "{", "{\'_type\':\'" + find + "\', ");
        }

        namePtrn = Pattern.compile("(\\w+)=");
        nameMtchr = namePtrn.matcher(typeData);

        while (nameMtchr.find()) {
            String find = nameMtchr.group(1);
            typeData = typeData.replace(" " + find + "=", "\'"+find+"\':");
        }

        typeData = typeData.replace("'", "\"");

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode object = mapper.readTree(typeData);

            return object;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

}
