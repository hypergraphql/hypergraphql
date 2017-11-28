package org.hypergraphql.datamodel;

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
import org.hypergraphql.config.HGQLConfig;

/**
 * Created by szymon on 22/08/2017.
 */

public class ModelContainer {

//    private static String queryValuesOfObjectPropertyTemp = "SELECT distinct ?object WHERE {<%s> <%s> ?object FILTER (!isLiteral(?object)) . }";
//    private static String queryValuesOfDataPropertyTemp = "SELECT distinct (str(?object) as ?value) WHERE {<%1$s> <%2$s> ?object  FILTER isLiteral(?object) . %3$s }";
    private static String querySubjectsOfObjectPropertyFilterTemp = "SELECT ?subject WHERE { ?subject <%1$s> <%2$s> . } ";

    protected Model model;

    protected static Logger logger = Logger.getLogger(ModelContainer.class);


    public ModelContainer(Model model) {

        this.model=model;

    }



    private ResultSet sparqlSelect(String queryString) {

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
