package uk.co.semanticintegration.hypergraphql;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.ARQConstants;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.Symbol;
import org.apache.log4j.Logger;

/**
 * Created by szymon on 22/08/2017.
 */
public class SparqlClient {

    private static String queryValuesOfObjectPropertyTemp = "SELECT distinct ?object WHERE {<%s> <%s> ?object FILTER (!isLiteral(?object)) . }";
    private static String queryValuesOfDataPropertyTemp = "SELECT distinct (str(?object) as ?value) WHERE {<%1$s> <%2$s> ?object  FILTER isLiteral(?object) . %3$s }";
    private static String querySubjectsOfObjectPropertyFilterTemp = "SELECT distinct ?subject WHERE { ?subject <http://hgql/root> <http://hgql/node_x> . ?subject <%1$s> <%2$s> . } ";
    private static String queryRootTypeTemp = "SELECT distinct ?object WHERE { <%1$s> <http://hgql/root>  ?object . <%1$s> <http://hgql/root>  <http://hgql/node_x>  FILTER (?object != <http://hgql/node_x> )} ";

    private Model model;

    private static Logger logger = Logger.getLogger(SparqlClient.class);



    public SparqlClient(List<String> queries, Map<String, Context> sparqlEndpointsContext) {

        model = ModelFactory.createDefaultModel();

        for (String constructQuery : queries) {

            logger.debug("Executing construct query: " + constructQuery);

            QueryExecution qexec = QueryExecutionFactory.create(constructQuery, model);

            Context mycxt = qexec.getContext();
            Symbol serviceContext = ARQConstants.allocSymbol("http://jena.hpl.hp.com/Service#", "serviceContext");
            mycxt.put(serviceContext, sparqlEndpointsContext);

            try {
                qexec.execConstruct(model);
            } catch (Exception e) {
                logger.error(e);
            }
        }

         // model.write(System.out);
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

    public String getRootTypeOfResource(RDFNode node) {
        String queryString = String.format(queryRootTypeTemp, node.asResource().getURI().toString());

        ResultSet queryResults = sparqlSelect(queryString);

        String result = null;

        if (queryResults != null && queryResults.hasNext()) {

            result = queryResults.next().get("?object").toString();
        }

        return result;

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