package org.hypergraphql.integration;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

class ApplicationTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(ApplicationTest.class);

    @Test
    void server_test() throws Exception {

        String construct = "PREFIX dbpedia: <http://dbpedia.org/ontology/>" +
                "PREFIX  foaf: <http://xmlns.com/foaf/0.1/>" +
                "PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#>" +
                "CONSTRUCT { ?person a <http://dbpedia.org/ontology/Person> . " +
                "?person <http://xmlns.com/foaf/0.1/name> ?name . ?person rdfs:label ?label }" +
                "WHERE\n" +
                "  { ?person a <http://dbpedia.org/ontology/Person> . " +
                "?person <http://xmlns.com/foaf/0.1/name> ?name . ?person rdfs:label ?label }\n" +
                "LIMIT   20\n" +
                "";

        Query query = QueryFactory.create(construct); //s2 = the query above
        QueryExecution qExe = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);

        Model model1 = qExe.execConstruct();

        LOGGER.debug(model1.toString());

        Property property = model1.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        RDFNode node = model1.createResource("http://dbpedia.org/ontology/Person");

        ResIterator nodes = model1.listResourcesWithProperty(property, node);

        Model model2 = ModelFactory.createDefaultModel();

        while (nodes.hasNext()) {

            ParameterizedSparqlString pss = new ParameterizedSparqlString();

            pss.setCommandText("PREFIX dbpedia: <http://dbpedia.org/ontology/>\n" +
                    "PREFIX  foaf: <http://xmlns.com/foaf/0.1/>\n" +
                    "PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                    "CONSTRUCT { ?person a dbpedia:Person . ?person dbpedia:birthPlace ?birthplace . ?birthplace a dbpedia:City . \n" +
                    "?birthplace rdfs:label ?birthplaceLabel  } \n" +
                    "WHERE\n" +
                    "  { ?person dbpedia:birthPlace ?birthplace . \n" +
                    "                   ?birthplace rdfs:label ?birthplaceLabel  }\n" +
                    "LIMIT   10");

            Resource currentNode = nodes.next();
            pss.setParam("person", currentNode);
            LOGGER.debug(pss.toString());
            query = QueryFactory.create(pss.toString());
            qExe = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);
            model2.add(qExe.execConstruct());
        }

        model2.write(System.out, "NTRIPLE");

        File file2 = new File("src/test/resources/TestServices/cities1.ttl");
        try (OutputStream stream = new FileOutputStream(file2)) {
            model2.write(stream, "TTL");
        }
    }
}
