import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.language.TypeDefinition;

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


    Map<String, String> globalContext;
    Map<String, String> JSONLD_VOC = new HashMap<String, String>() {{
        put("_context", "@context");
        put("_id", "@id");
        put("_value", "@value");
        put("_type", "@type");
        put("_language", "@language");
        put("_graph", "@graph");
    }}; //from graphql names to jsonld reserved names


    public Converter(Config config) {
        this.globalContext = config.context;
    }

    class Traversal {
        Object data;
        Map<String, String> context;

    }

    public Set<String> graphql2sparql (String query) {

        //this method will convert a given graphql query into a nested SPARQL construct query
        // that will retrieve all relevant data in one go and put it into an in-memory jena store

        Set<String> output = new HashSet<>();

        String topObjectPropertyTemplate = "CONSTRUCT { ?x <%1$s> <%2$s>  . \n %3$s } WHERE { ?x <%1$s> <%2$s> . \n %4$s }";
        String topDataPropertyTemplate = "CONSTRUCT { ?x <%1$s> ?value . \n %3$s } WHERE { ?x <%1$s> ?value . FILTER (str(?value)=\"%2$s\"). \n %4$s }";

        JsonNode jsonQuery = query2json(query);

      //  System.out.println(jsonQuery.toString());

        for (JsonNode root : jsonQuery) {
            String[] triplePatterns = getSubquery(root, "?x");

            String constructQuery = null;

            if (root.get("args").has("uri")) {

                constructQuery = String.format(topObjectPropertyTemplate,
                        globalContext.get(root.get("name").asText()),
                        globalContext.get(root.get("args").get("uri").asText()),
                        triplePatterns[0],
                        triplePatterns[1]);

            }

            if (root.get("args").has("value")) {

                constructQuery = String.format(topDataPropertyTemplate,
                        globalContext.get(root.get("name").asText()),
                        root.get("args").get("value").asText(),
                        triplePatterns[0],
                        triplePatterns[1]);

            }

        //    System.out.println(constructQuery);

            output.add(constructQuery);
        }

        return output;
    }

    String[] getSubquery(JsonNode node, String parentVar) {

        String constructPattern = "%1$s <%2$s> %3$s . \n ";
        String optionalPattern = "OPTIONAL { %1$s <%2$s> %3$s . \n%4$s } ";

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

                n++;

                String childVar = parentVar + "_" + n;

                String[] grandChildPatterns = getSubquery(field, childVar);

                String childConstructPattern = String.format(constructPattern,
                        parentVar,
                        globalContext.get(field.get("name").asText()),
                        childVar
                        );

                String childOptionalPattern = String.format(optionalPattern,
                        parentVar,
                        globalContext.get(field.get("name").asText()),
                        childVar,
                        grandChildPatterns[1]
                );

                childConstruct.add(childConstructPattern);
                childConstruct.add(grandChildPatterns[0]);

                childOptional.add(childOptionalPattern);
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
            namePtrn = Pattern.compile("(\\w+)\\s<([\\w\\s:\"_,}{\\[\\]]*)>");
            nameMtchr = namePtrn.matcher(query);

            query=query.replaceAll("(\\w+)\\s<([\\w\\s:\"_,}{\\[\\]]*)>", "{\"name\":\"$1\", \"fields\":[$2]}");

        } while (nameMtchr.find());

        do {
        namePtrn = Pattern.compile("(\\w+)\\s\\(([\\w\\s:\"_]*)\\)\\s<([\\w\\s:\"_},{\\[\\]]*)>");
        nameMtchr = namePtrn.matcher(query);

        query=query.replaceAll("(\\w+)\\s\\(([\\w\\s:\"_]*)\\)\\s<([\\w\\s:\"_},{\\[\\]]*)>", "{\"name\":\"$1\", \"args\":{$2}, \"fields\":[$3]}");

        } while (nameMtchr.find());

        query = query
                .replaceAll("(\\w+) ", " {\"name\":\"$1\"} ")
                .replaceAll("\\s(\\w+):", " \"$1\":")
                .replaceAll("}\\s*\\{", "}, {")
                .replaceAll("<", "[")
                .replaceAll(">", "]");

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode object = mapper.readTree(query);

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

                Converter.Traversal restructured = jsonRewrite(dataObject.get(field));

                if (restructured.data.getClass() == ArrayList.class) graphData.addAll((ArrayList<Object>) restructured.data);
                if (restructured.data.getClass() == LinkedHashMap.class) graphData.add(restructured.data);

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
            restructured.data = dataObject;
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
                    context.put(key, globalContext.get(key));
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
