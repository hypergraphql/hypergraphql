import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by szymon on 22/08/2017.
 */
public class SparqlClient {

    static String SPARQL_ENDPOINT = "http://zbw.eu/beta/sparql/stw/query";
    static String queryOutgoingTemplate = "SELECT distinct ?target WHERE {<%s> <%s> ?target} LIMIT %s";
    static String queryInstancesTemplate = "SELECT distinct ?instance WHERE {?instance a <%s>} LIMIT %s";
    static int defaultLimit = 10000;


    public static ResultSet sparqlSelect(String queryString, String endpoint) {
        System.out.println(queryString);
        Query query = QueryFactory.create(queryString);

        QueryExecution qexec = QueryExecutionFactory.sparqlService(endpoint, query);
        try {
            ResultSet results = qexec.execSelect();
            return results;
        } catch (Exception e) {
            System.out.println("Failed to return the response on query: " + queryString);
            System.out.println(e);
            return null;
        }
    }


    public static List<RDFNode> getOutgoingEdge(String node, String predicate, int limit, String endpoint) {

        limit = (limit==0)? defaultLimit : limit;

        String queryString = String.format(queryOutgoingTemplate, node, predicate, limit);

        ResultSet queryResults = sparqlSelect(queryString, endpoint);

        if (queryResults!=null) {
            List<RDFNode> uriList = new ArrayList<>();

            while (queryResults.hasNext()) {
                QuerySolution nextSol = queryResults.nextSolution();
                RDFNode target = nextSol.get("?target");
                uriList.add(target);
            }
            return uriList;
        }
        else return null;
    }

    public static List<String> getInstances(String type, int limit, String endpoint) {

        limit = (limit==0)? defaultLimit : limit;

        String queryString = String.format(queryInstancesTemplate, type, limit);

        ResultSet queryResults = sparqlSelect(queryString, endpoint);

        if (queryResults!=null) {
            List<String> uriList = new ArrayList<>();

            while (queryResults.hasNext()) {
                QuerySolution nextSol = queryResults.nextSolution();
                String instance = nextSol.get("?instance").toString();
                uriList.add(instance);
            }
            return uriList;
        }
        else return null;
    }
}