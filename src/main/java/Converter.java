import java.util.*;

/**
 * Created by szymon on 15/09/2017.
 *
 * This class contains rewrite methods between different query/response formats
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

    public String graphql2sparql (String query) {

        //this method will convert a given graphql query into a nested SPARQL construct query
        // that will retrieve all relevant data in one go and put it into an in-memory jena store

        return null;
    }

    public Object graphql2jsonld (Map<String, Object> dataObject) {



            ArrayList<Object> graphData = new ArrayList<>();
            Map<String,String> graphContext = new HashMap<>();

            Iterator<String> queryFields = dataObject.keySet().iterator();

            while (queryFields.hasNext()) {

                String field = queryFields.next();

                Converter.Traversal restructured = rewrite(dataObject.get(field));

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


    public Converter.Traversal rewrite (Object dataObject) {

        if (dataObject.getClass()==ArrayList.class) {

            //System.out.println("It's an array");

            ArrayList<Object> newList = new ArrayList<>();
            Map<String, String> context = new HashMap<>();
            for (Object item : (ArrayList<Object>) dataObject) {
                Converter.Traversal itemRes = rewrite(item);
                newList.add(itemRes.data);
                context.putAll(itemRes.context);
            }
            Converter.Traversal restructured = new Converter.Traversal();
            restructured.data = newList;
            restructured.context = context;

            return restructured;
        }

        if (dataObject.getClass()!=LinkedHashMap.class) {

            // System.out.println("It's not an array nor a map.");

            Converter.Traversal restructured = new Converter.Traversal();
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
                Converter.Traversal childRes = rewrite(child);
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
}
