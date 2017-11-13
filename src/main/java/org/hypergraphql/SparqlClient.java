package org.hypergraphql;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.*;
import org.apache.log4j.Logger;

/**
 * Created by szymon on 22/08/2017.
 */
public class SparqlClient {

    private static String queryValuesOfObjectPropertyTemp = "SELECT distinct ?object WHERE {<%s> <%s> ?object FILTER (!isLiteral(?object)) . }";
    private static String queryValuesOfDataPropertyTemp = "SELECT distinct (str(?object) as ?value) WHERE {<%1$s> <%2$s> ?object  FILTER isLiteral(?object) . %3$s }";
    private static String querySubjectsOfObjectPropertyFilterTemp = "SELECT ?subject WHERE { ?subject <%1$s> <%2$s> . } ";

    private Model model;

    private static Logger logger = Logger.getLogger(SparqlClient.class);


    public SparqlClient(List<Map<String, String>> queryRequests, Config config) {

        final String MATCH_PARENT = "SELECT (GROUP_CONCAT(CONCAT(\"<\",str(%s),\">\");separator=\" \") as ?uris) WHERE { %s }";
        final String VALUES_PATTERN = "VALUES %s { %s }";
        model = ModelFactory.createDefaultModel();

        for (Map<String, String> queryRequest : queryRequests) {

            String query = queryRequest.get("query");
            String service = queryRequest.get("service");
            String match = queryRequest.get("match");
            String var = queryRequest.get("var");

            if (match.equals("")) {
                query = String.format(query, "");
            } else {
                String matchQuery = String.format(MATCH_PARENT, var, match);

                ResultSet results = sparqlSelect(matchQuery);
                String vals = results.next().get("?uris").toString();

                query = String.format(query, String.format(VALUES_PATTERN, var, vals));
            }

            logger.debug("Executing construct query: " + query);

            try {
                HttpResponse<InputStream> resp = Unirest.get("http://dbpedia.org/sparql")
                        .queryString("query", query)
                        .header("accept", "application/rdf+xml")
                        .basicAuth(config.serviceUsr(service), config.servicePswd(service))
                        .asBinary();

                Model next = ModelFactory.createDefaultModel();

                InputStream in = resp.getBody();

                next.read(in, null, "RDF/XML");

                model.add(next);

            } catch (UnirestException e) {
                logger.error(e);
            }
        }
        //  model.write(System.out);
    }

    public ResultSet sparqlSelect(String queryString) {

        logger.debug("Executing SPARQL fetchquery: " + queryString);
        QueryExecution qexec = QueryExecutionFactory.create(queryString, model);
        try {
            ResultSet results = qexec.execSelect();
            return results;
        } catch (Exception e) {
            logger.error("Failed to return the response on query: " + queryString);
            logger.error(e);
            return null;
        }
    }

    public List<RDFNode> getValuesOfObjectProperty(String subject, String predicate, Map<String, Object> args) {

        String queryString = String.format(queryValuesOfObjectPropertyTemp, subject, predicate);
        ResultSet queryResults = sparqlSelect(queryString);

        if (queryResults != null) {

            List<RDFNode> uriList = new ArrayList<>();

            while (queryResults.hasNext()) {
                QuerySolution nextSol = queryResults.nextSolution();
                RDFNode object = nextSol.get("?object");
                uriList.add(object);
            }
            return uriList;
        }
        return null;
    }

    public RDFNode getValueOfObjectProperty(String subject, String predicate, Map<String, Object> args) {

        String queryString = String.format(queryValuesOfObjectPropertyTemp, subject, predicate);
        ResultSet queryResults = sparqlSelect(queryString);

        if (queryResults != null && queryResults.hasNext()) {

            QuerySolution nextSol = queryResults.nextSolution();
            return nextSol.get("?object");
        }
        return null;
    }

    public List<Object> getValuesOfDataProperty(String subject, String predicate, Map<String, Object> args) {

        String langFilter = "";

        if (args.containsKey("lang")) langFilter = "FILTER (lang(?object)=\"" + args.get("lang").toString() + "\") ";

        String queryString = String.format(queryValuesOfDataPropertyTemp, subject, predicate, langFilter);
        ResultSet queryResults = sparqlSelect(queryString);

        if (queryResults != null) {

            List<Object> valList = new ArrayList<>();

            while (queryResults.hasNext()) {
                QuerySolution nextSol = queryResults.nextSolution();
                String value = nextSol.get("?value").toString();
                valList.add(value);
            }
            return valList;
        } else {
            return null;
        }
    }

    public String getValueOfDataProperty(String subject, String predicate, Map<String, Object> args) {

        String langFilter = "";

        if (args.containsKey("lang")) langFilter = "FILTER (lang(?object)=\"" + args.get("lang").toString() + "\") ";

        String queryString = String.format(queryValuesOfDataPropertyTemp, subject, predicate, langFilter);
        ResultSet queryResults = sparqlSelect(queryString);

        if (queryResults != null && queryResults.hasNext()) {

            QuerySolution nextSol = queryResults.nextSolution();
            String value = nextSol.get("?value").toString();

            return value;
        } else {
            return null;
        }
    }


    public List<RDFNode> getSubjectsOfObjectPropertyFilter(String predicate, String uri) {

        String queryString = String.format(querySubjectsOfObjectPropertyFilterTemp, predicate, uri);

        ResultSet queryResults = sparqlSelect(queryString);

        if (queryResults != null) {

            List<RDFNode> uriList = new ArrayList<>();

            while (queryResults.hasNext()) {
                QuerySolution nextSol = queryResults.nextSolution();
                RDFNode subject = nextSol.get("?subject");
                uriList.add(subject);
            }
            return uriList;
        } else {
            return null;
        }
    }
}