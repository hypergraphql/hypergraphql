import java.util.*;

/**
 * Created by szymon on 05/09/2017.
 */
public class Graphql2Jsonld {

    Map<String, String> globalContext;
    Map<String, String> JSONLD_VOC = new HashMap<String, String>() {{
        put("_context", "@context");
        put("_id", "@id");
        put("_value", "@value");
        put("_type", "@type");
        put("_language", "@language");
        put("_graph", "@graph");
    }}; //from graphql names to jsonld reserved names


    public Graphql2Jsonld(Config config) {
        this.globalContext = config.context;
    }

    class Traversal {
        Object data;
        Map<String, String> context;

    }

    public Object graphql2jsonld (Map<String, Object> dataObject) {

        if (dataObject.containsKey("_graph")) {
            Traversal restructured = restructure(dataObject.get("_graph"));
            Map<String, Object> output = new HashMap<>();

            output.put("@graph", restructured.data);
            output.put("@context", restructured.context);

            System.out.println(output.toString());

            return output;
        }

        return null;
    }

    public Traversal restructure (Object dataObject) {

        if (dataObject.getClass()==ArrayList.class) {

            //System.out.println("It's an array");

            ArrayList<Object> newList = new ArrayList<>();
            Map<String, String> context = new HashMap<>();
            for (Object item : (ArrayList<Object>) dataObject) {
                Traversal itemRes = restructure(item);
                newList.add(itemRes.data);
                context.putAll(itemRes.context);
            }
            Traversal restructured = new Traversal();
            restructured.data = newList;
            restructured.context = context;

            return restructured;
        }

        if (dataObject.getClass()!=LinkedHashMap.class) {

            // System.out.println("It's not an array nor a map.");

            Traversal restructured = new Traversal();
            restructured.data = dataObject;
            restructured.context = new HashMap<>();

            return restructured;
        } else {

            //System.out.println("It's a map.");

            Map<String, Object> mapObject = (Map<String, Object>) dataObject;
            Map<String, Object> targetObject = new HashMap<>();

            Set<String> keys = mapObject.keySet();

            Map<String, String> context = new HashMap<>();

            for (String key: keys) {
                Object child = mapObject.get(key);
                Traversal childRes = restructure(child);
                if (JSONLD_VOC.containsKey(key)) targetObject.put(JSONLD_VOC.get(key), childRes.data);
                else {
                    targetObject.put(key, childRes.data);
                    context.put(key, globalContext.get(key));
                }
                context.putAll(childRes.context);
            }

            Traversal restructured = new Traversal();
            restructured.data = targetObject;
            restructured.context = context;

            return restructured;
        }

    }

}
