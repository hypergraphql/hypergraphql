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
import org.apache.jena.riot.RDFDataMgr;
import org.hypergraphql.Application;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static org.apache.jena.riot.Lang.TURTLE;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(ApplicationTest.class);

    private static final String OUTPUT_FILE_PATH = "src/test/resources/test_services/output/cities1.ttl";

    @AfterAll
    static void cleanUp() {

        final File outputTurtle = new File(OUTPUT_FILE_PATH);
        outputTurtle.deleteOnExit();
    }

    @Test
    void startup_test() throws Exception {

        final String[] args = {"-config", "src/test/resources/config.json"};
        Application.main(args);
        Application.stop();
    }

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

        Query query = QueryFactory.create(construct);
        QueryExecution qExe = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);

        Model constructedModel = qExe.execConstruct();

        LOGGER.debug(constructedModel.toString());

        Property property = constructedModel.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        RDFNode node = constructedModel.createResource("http://dbpedia.org/ontology/Person");

        ResIterator nodes = constructedModel.listResourcesWithProperty(property, node);

        Model fromDBPedia = ModelFactory.createDefaultModel();

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
            fromDBPedia.add(qExe.execConstruct());
        }

        fromDBPedia.write(System.out, "NTRIPLE");

        File outputTurtle = new File(OUTPUT_FILE_PATH);
        try (OutputStream stream = new FileOutputStream(outputTurtle)) {
            fromDBPedia.write(stream, "TTL");
        }

        final InputStream citiesTurtle = getClass().getClassLoader().getResourceAsStream("test_services/cities.ttl");

        // read from this file to verify something happened then delete it
        assertTrue(modelsAreEquivalent(citiesTurtle, new FileInputStream(outputTurtle)));
    }

    private boolean modelsAreEquivalent(final InputStream lhsStream, final InputStream rhsStream) {

        final Model lhs = ModelFactory.createDefaultModel();
        final Model rhs = ModelFactory.createDefaultModel();

        RDFDataMgr.read(lhs, lhsStream, "", TURTLE);
        RDFDataMgr.read(rhs, rhsStream, "", TURTLE);

        return lhs.isIsomorphicWith(rhs) && rhs.isIsomorphicWith(lhs);
    }
}
