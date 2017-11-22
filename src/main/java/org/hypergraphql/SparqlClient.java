package org.hypergraphql;

import java.io.ByteArrayInputStream;
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

//    private static String queryValuesOfObjectPropertyTemp = "SELECT distinct ?object WHERE {<%s> <%s> ?object FILTER (!isLiteral(?object)) . }";
//    private static String queryValuesOfDataPropertyTemp = "SELECT distinct (str(?object) as ?value) WHERE {<%1$s> <%2$s> ?object  FILTER isLiteral(?object) . %3$s }";
    private static String querySubjectsOfObjectPropertyFilterTemp = "SELECT ?subject WHERE { ?subject <%1$s> <%2$s> . } ";

    protected Model model;

    protected static Logger logger = Logger.getLogger(SparqlClient.class);


    public SparqlClient(List<Map<String, String>> queryRequests, Config config) {

        final String MATCH_PARENT = "SELECT (GROUP_CONCAT(CONCAT(\"<\",str(%s),\">\");separator=\" \") as ?uris) WHERE { %s }";
        final String VALUES_PATTERN = "VALUES %s { %s }";

        model = ModelFactory.createDefaultModel();

        for (Map<String, String> queryRequest : queryRequests) {

            String query = queryRequest.get("query");
            String service = queryRequest.get("service");
            String match = queryRequest.get("match");
            String var = queryRequest.get("var");

            if (!match.equals("")) {

                String matchQuery = String.format(MATCH_PARENT, var, match);

                ResultSet results = sparqlSelect(matchQuery);
                String vals = results.next().get("?uris").toString();

                query = String.format(query, String.format(VALUES_PATTERN, var, vals));

            }

            logger.debug("Executing construct query: " + query);

            Unirest.setTimeouts(0, 0);


            try {

                HttpResponse<InputStream> resp = Unirest.get(service)
                        .queryString("query", query)
                        .header("accept", "application/rdf+xml")
                        .basicAuth(config.services().get(service).user(), config.services().get(service).password())
                        .asBinary();

                Model next = ModelFactory.createDefaultModel();

                InputStream in = resp.getBody();


                next.read(in, null, "RDF/XML");

                model.add(next);

            } catch (UnirestException e) {
                logger.error(e);
            }
        }

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


    public Property getPropertyFromUri(String string ) {

        return this.model.getProperty(string);

    }


//    public List<RDFNode> getSubjectsOfObjectPropertyFilter(Property predicate) {
//
//
//        ResIterator iterator = this.model.listSubjectsWithProperty(predicate);
//        List<RDFNode> nodeList = new ArrayList<>();
//
//
//        while (iterator.hasNext()) {
//
//            nodeList.add(iterator.next());
//        }
//
//        return nodeList;
//
//    }

    public String getValueOfDataProperty(Resource subject, Property predicate, Map<String, Object> args) {



        NodeIterator iterator = this.model.listObjectsOfProperty(subject,  predicate);


        while (iterator.hasNext()) {


            RDFNode data = iterator.next();


            if (data.isLiteral()) {

                if (args.containsKey("lang"))

                {
                    if (data.asLiteral().getLanguage().toString().equals(args.get("lang").toString()))
                        return data.asLiteral().getString();
                } else {

                    return data.asLiteral().getString();
                }

            }

        }


        return null;
    }

    public List<Object> getValuesOfDataProperty(Resource subject, Property predicate, Map<String, Object> args) {

        List<Object> valList = new ArrayList<>();



        NodeIterator iterator = this.model.listObjectsOfProperty(subject, predicate);

        while (iterator.hasNext()) {


            RDFNode data = iterator.next();


            if (data.isLiteral()) {
                if (args.containsKey("lang")) {
                    if (data.asLiteral().getLanguage().toString().equals(args.get("lang").toString()))
                        valList.add(data.asLiteral().getString());
                } else {

                    valList.add(data.asLiteral().getString());

                }
            }
        }

        return valList;


    }

    public List<RDFNode> getValuesOfObjectProperty(Resource subject, Property predicate, Map<String, Object> args) {



        NodeIterator iterator = this.model.listObjectsOfProperty(subject, predicate);
        List<RDFNode> uriList = new ArrayList<>();


        while (iterator.hasNext()) {

            RDFNode next = iterator.next();

            if (!next.isLiteral()) uriList.add(next);
        }

        return uriList;

    }

    public RDFNode getValueOfObjectProperty(Resource subject, Property predicate, Map<String, Object> args) {


        NodeIterator iterator = this.model.listObjectsOfProperty(subject, predicate);
        while (iterator.hasNext()) {

            RDFNode next = iterator.next();

            if (!next.isLiteral()) return next;
        }
        return null;

    }
}
