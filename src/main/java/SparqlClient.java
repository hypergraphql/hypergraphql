import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.jena.atlas.web.auth.HttpAuthenticator;
import org.apache.jena.atlas.web.auth.PreemptiveBasicAuthenticator;
import org.apache.jena.atlas.web.auth.SimpleAuthenticator;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.ARQConstants;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.Symbol;

import java.io.ByteArrayInputStream;
import java.util.*;

/**
 * Created by szymon on 22/08/2017.
 */
public class SparqlClient {

    static String queryValuesOfObjectPropertyTemp = "SELECT distinct ?object WHERE {<%s> <%s> ?object . }";
    static String queryValuesOfDataPropertyTemp = "SELECT distinct (str(?object) as ?value) WHERE {<%1$s> <%2$s> ?object . %3$s }";
    //static String querySubjectsOfDataPropertyFilterTemp = "SELECT distinct ?subject WHERE {?subject <%1$s> ?value . FILTER (str(?value)=\"%2$s\") %3$s }";
    static String querySubjectsOfObjectPropertyFilterTemp = "SELECT distinct ?subject WHERE { ?subject a <root> . ?subject <%1$s> <%2$s> . } ";

    Model model;

    public SparqlClient(Set<String> queries, Map<String, Context> sparqlEndpointsContext) {

        model = ModelFactory.createDefaultModel();
//        String testQuery =
//                "CONSTRUCT \n" +
//                        "  { ?x a <root> . " +
//                        "?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Person> .\n" +
//                        "   ?x <http://dbpedia.org/ontology/spouse> ?s .\n" +
//                        "    ?s <http://xmlns.com/foaf/0.1/name> ?x_1 .\n" +
//                        "    }\n" +
//                        "WHERE\n" +
//                        "  { \n" +
//                        "    SERVICE <http://live.dbpedia.org/sparql/>\n" +
//                        "      { \n" +
//                        "        { \n" +
//                        "          SELECT  ?x\n" +
//                        "          WHERE\n" +
//                        "            { ?x a <http://dbpedia.org/ontology/Person> \n" +
//                        "              OPTIONAL {?x <http://dbpedia.org/ontology/spouse> ?s .}\n" +
//                        "            }\n" +
//                        "          LIMIT 10\n" +
//                        "        }\n" +
//                        "      }\n" +
//                        "FILTER ( bound(?s) )"+
//                        "    SERVICE <http://dbpedia.org/sparql/>\n" +
//                        "        { \n" +
//                        "          GRAPH <http://dbpedia.org>\n" +
//                        "            { \n" +
//                        "              OPTIONAL { \n" +
//                        "              ?s <http://xmlns.com/foaf/0.1/name>  ?x_1 . }\n" +
//                        "            }\n" +
//                        "        }\n" +
//                        "  }";
//        queries = new HashSet<>();
//        queries.add(testQuery);
        for (String constructQuery : queries) {
            QueryExecution qexec = QueryExecutionFactory.create(constructQuery, model);

            Context mycxt = qexec.getContext();
            Symbol serviceContext = ARQConstants.allocSymbol("http://jena.hpl.hp.com/Service#", "serviceContext");
            mycxt.put(serviceContext, sparqlEndpointsContext);

            try {
                model.add(qexec.execConstruct());
            } catch (Exception e) {
                System.out.println(e.fillInStackTrace());
            }
        }
        model.write(System.out);
    }

    public ResultSet sparqlSelect(String queryString) {

        System.out.println(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(queryString, model);
        try {
            ResultSet results = qexec.execSelect();
            return results;
        } catch (Exception e) {
            System.out.println("Failed to return the response on query: " + queryString);
            System.out.println(e);
            return null;
        }
    }

    public List<RDFNode> getValuesOfObjectProperty(String subject, String predicate, Map<String, Object> args) {

        String queryString = String.format(queryValuesOfObjectPropertyTemp, subject, predicate);
        ResultSet queryResults = sparqlSelect(queryString);

        if (queryResults!=null) {

                List<RDFNode> uriList = new ArrayList<>();

                while (queryResults.hasNext()) {
                    QuerySolution nextSol = queryResults.nextSolution();
                    RDFNode object = nextSol.get("?object");
                    uriList.add(object);
                }
                return uriList;
        }
        else return null;
    }

    public RDFNode getValueOfObjectProperty(String subject, String predicate, Map<String, Object> args) {

        String queryString = String.format(queryValuesOfObjectPropertyTemp, subject, predicate);
        ResultSet queryResults = sparqlSelect(queryString);

        if (queryResults!=null  && queryResults.hasNext() ) {

            QuerySolution nextSol = queryResults.nextSolution();
            RDFNode object = nextSol.get("?object");

            return object;
        }

        else return null;
    }

    public List<Object> getValuesOfDataProperty(String subject, String predicate, Map<String, Object> args) {

        String langFilter = "";

        if (args.containsKey("lang")) langFilter = "FILTER (lang(?object)=\""+args.get("lang").toString()+"\") ";

        String queryString = String.format(queryValuesOfDataPropertyTemp, subject, predicate, langFilter);
        ResultSet queryResults = sparqlSelect(queryString);

        if (queryResults!=null) {

            List<Object> valList = new ArrayList<>();

            while (queryResults.hasNext()) {
                QuerySolution nextSol = queryResults.nextSolution();
                String value = nextSol.get("?value").toString();
                valList.add(value);
            }
            return valList;
        }
        else return null;
    }

    public String getValueOfDataProperty(String subject, String predicate, Map<String, Object> args) {

        String langFilter = "";

        if (args.containsKey("lang")) langFilter = "FILTER (lang(?object)=\""+args.get("lang").toString()+"\") ";

        String queryString = String.format(queryValuesOfDataPropertyTemp, subject, predicate, langFilter);
        ResultSet queryResults = sparqlSelect(queryString);

        if (queryResults!=null && queryResults.hasNext()) {

            QuerySolution nextSol = queryResults.nextSolution();
            String value = nextSol.get("?value").toString();

            return value;
        }
        else return null;
    }

    /*
    public List<RDFNode> getSubjectsOfDataPropertyFilter(String predicate, String value) {

        String queryString = String.format(querySubjectsOfDataPropertyFilterTemp, predicate, value);
        ResultSet queryResults = sparqlSelect(queryString);

        if (queryResults!=null) {

            List<RDFNode> uriList = new ArrayList<>();

            while (queryResults.hasNext()) {
                QuerySolution nextSol = queryResults.nextSolution();
                RDFNode subject = nextSol.get("?subject");
                uriList.add(subject);
            }
            return uriList;
        }
        else return null;
    }

    public RDFNode getSubjectOfDataPropertyFilter(String predicate, String value) {

        String queryString = String.format(querySubjectsOfDataPropertyFilterTemp, predicate, value);
        ResultSet queryResults = sparqlSelect(queryString);

        if (queryResults!=null && queryResults.hasNext() ) {

                QuerySolution nextSol = queryResults.nextSolution();
                RDFNode subject = nextSol.get("?subject");

            return subject;
        }
        else return null;
    }
    */


    public List<RDFNode> getSubjectsOfObjectPropertyFilter(String predicate, String uri, Map<String, Object> args) {

        String queryString = String.format(querySubjectsOfObjectPropertyFilterTemp, predicate, uri);

        ResultSet queryResults = sparqlSelect(queryString);

        if (queryResults!=null) {

            List<RDFNode> uriList = new ArrayList<>();

            while (queryResults.hasNext()) {
                QuerySolution nextSol = queryResults.nextSolution();
                RDFNode subject = nextSol.get("?subject");
                uriList.add(subject);
            }
            return uriList;
        }
        else return null;
    }

    /*public RDFNode getSubjectOfObjectPropertyFilter(String predicate, String uri) {

        String queryString = String.format(querySubjectsOfObjectPropertyFilterTemp, predicate, uri);
        ResultSet queryResults = sparqlSelect(queryString);

        if (queryResults!=null && queryResults.hasNext() ) {

            QuerySolution nextSol = queryResults.nextSolution();
            RDFNode subject = nextSol.get("?subject");

            return subject;
        }
        else return null;
    }
    */

}