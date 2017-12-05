package org.hypergraphql.datamodel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.*;
import org.apache.log4j.Logger;

/**
 * Created by szymon on 22/08/2017.
 */

public class ModelContainer {

    protected Model model;

    //protected static Logger logger = Logger.getLogger(ModelContainer.class);


    public ModelContainer(Model model) {

        this.model=model;

    }


    public List<RDFNode> getRootResources(String subjectURI, String predicateURI) {

        Property property = model.createProperty(predicateURI);
        Resource subject = model.getResource(subjectURI);

        List<RDFNode> uriList = new ArrayList<>();

        NodeIterator iterator = this.model.listObjectsOfProperty(subject, property);

        while (iterator.hasNext()) {

            RDFNode object = iterator.next();

            uriList.add(object);
        }

        return uriList;

    }


    public Property getPropertyFromUri(String string ) {

        return this.model.getProperty(string);

    }

    public String getValueOfDataProperty(Resource subject, Property predicate, Map<String, Object> args) {



        NodeIterator iterator = this.model.listObjectsOfProperty(subject, predicate);


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
