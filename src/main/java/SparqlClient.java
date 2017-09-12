import org.apache.jena.atlas.web.auth.HttpAuthenticator;
import org.apache.jena.atlas.web.auth.PreemptiveBasicAuthenticator;
import org.apache.jena.atlas.web.auth.SimpleAuthenticator;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by szymon on 22/08/2017.
 */
public class SparqlClient {

    static String queryValuesOfObjectPropertyTemp = "SELECT distinct ?object WHERE {<%s> <%s> ?object}";
    static String queryValuesOfDataPropertyTemp = "SELECT distinct (str(?object) as ?value) WHERE {<%s> <%s> ?object .}";
    static String querySubjectsOfDataPropertyFilterTemp = "SELECT distinct ?subject WHERE {?subject <%s> ?value . FILTER (str(?value)=\"%s\") }";
    static String querySubjectsOfObjectPropertyFilterTemp = "SELECT distinct ?subject WHERE {?subject <%s> <%s>}";

    String endpoint;
    HttpAuthenticator authenticator;

    public SparqlClient(Config config) {
        endpoint = config.sparql.endpoint;
        authenticator = new PreemptiveBasicAuthenticator(new SimpleAuthenticator(config.sparql.user, config.sparql.password.toCharArray()),false);
    }

    public ResultSet sparqlSelect(String queryString) {
        System.out.println(queryString);
        Query query = QueryFactory.create(queryString);

        QueryExecution qexec = QueryExecutionFactory.sparqlService(endpoint, query, authenticator);
        try {
            ResultSet results = qexec.execSelect();
            return results;
        } catch (Exception e) {
            System.out.println("Failed to return the response on query: " + queryString);
            System.out.println(e);
            return null;
        }
    }

    public List<RDFNode> getValuesOfObjectProperty(String subject, String predicate) {

        String queryString = String.format(queryValuesOfObjectPropertyTemp, subject, predicate);
        ResultSet queryResults = sparqlSelect(queryString);

        if (queryResults!=null) {

                List<RDFNode> uriList = new ArrayList<>();

                while (queryResults.hasNext()) {
                    QuerySolution nextSol = queryResults.nextSolution();
                    System.out.println(nextSol.toString());
                    RDFNode object = nextSol.get("?object");
                    uriList.add(object);
                }
                return uriList;
        }
        else return null;
    }

    public RDFNode getValueOfObjectProperty(String subject, String predicate) {

        String queryString = String.format(queryValuesOfObjectPropertyTemp, subject, predicate);
        ResultSet queryResults = sparqlSelect(queryString);

        if (queryResults!=null) {

            QuerySolution nextSol = queryResults.nextSolution();
            RDFNode object = nextSol.get("?object");

            return object;
        }

        else return null;
    }

    public List<String> getValuesOfDataProperty(String subject, String predicate) {

        String queryString = String.format(queryValuesOfDataPropertyTemp, subject, predicate);
        ResultSet queryResults = sparqlSelect(queryString);

        if (queryResults!=null) {

            List<String> valList = new ArrayList<>();

            while (queryResults.hasNext()) {
                QuerySolution nextSol = queryResults.nextSolution();
                String value = nextSol.get("?value").toString();
                valList.add(value);
            }
            return valList;
        }
        else return null;
    }

    public String getValueOfDataProperty(String subject, String predicate) {

        String queryString = String.format(queryValuesOfDataPropertyTemp, subject, predicate);
        ResultSet queryResults = sparqlSelect(queryString);

        if (queryResults!=null) {

            QuerySolution nextSol = queryResults.nextSolution();
            String value = nextSol.get("?value").toString();

            return value;
        }

        else return null;
    }

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

        if (queryResults!=null) {

                QuerySolution nextSol = queryResults.nextSolution();
                RDFNode subject = nextSol.get("?subject");

            return subject;
        }
        else return null;
    }


    public List<RDFNode> getSubjectsOfObjectPropertyFilter(String predicate, String uri) {

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

    public RDFNode getSubjectOfObjectPropertyFilter(String predicate, String uri) {

        String queryString = String.format(querySubjectsOfObjectPropertyFilterTemp, predicate, uri);
        ResultSet queryResults = sparqlSelect(queryString);

        if (queryResults!=null) {

            QuerySolution nextSol = queryResults.nextSolution();
            RDFNode subject = nextSol.get("?subject");

            return subject;
        }
        else return null;
    }


}