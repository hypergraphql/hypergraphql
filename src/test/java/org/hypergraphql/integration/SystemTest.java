package org.hypergraphql.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.jena.fuseki.embedded.FusekiServer;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.SelectorImpl;
import org.hypergraphql.Controller;
import org.hypergraphql.config.system.HGQLConfig;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemTest {


    @Test
    void integration_test() {

        Model mainModel = ModelFactory.createDefaultModel();
        mainModel.read("src/test/resources/TestServices/dbpedia.ttl", "TTL");

        Model citiesModel = ModelFactory.createDefaultModel();
        citiesModel.read("src/test/resources/TestServices/cities.ttl", "TTL");
        Model expectedModel = ModelFactory.createDefaultModel();
        expectedModel.add(mainModel).add(citiesModel);

        Dataset ds = DatasetFactory.createTxnMem();
        ds.setDefaultModel(citiesModel);
        FusekiServer server = FusekiServer.create()
                .add("/ds", ds)
                .build()
                .start();

        HGQLConfig externalConfig = new HGQLConfig("src/test/resources/TestServices/externalconfig.json");

        Controller externalController = new Controller();
        externalController.start(externalConfig);

        HGQLConfig config = new HGQLConfig("src/test/resources/TestServices/mainconfig.json");

        Controller controller = new Controller();
        controller.start(config);

        ObjectMapper mapper = new ObjectMapper();

        ObjectNode bodyParam = mapper.createObjectNode();

        bodyParam.put("query", "{\n" +
                "  Person_GET {\n" +
                "    _id\n" +
                "    label\n" +
                "    name\n" +
                "    birthPlace {\n" +
                "      _id\n" +
                "      label\n" +
                "    }\n" +
                "    \n" +
                "  }\n" +
                "  City_GET {\n" +
                "    _id\n" +
                "    label}\n" +
                "}");

        Model returnedModel = ModelFactory.createDefaultModel();

        try {
            HttpResponse<InputStream> response = Unirest.post("http://localhost:8080/graphql")
                    .header("Accept", "application/rdf+xml")
                    .body(bodyParam.toString())
                    .asBinary();


            returnedModel.read(response.getBody(), "RDF/XML");

        } catch (UnirestException e) {
            e.printStackTrace();
        }

        Resource res = ResourceFactory.createResource("http://hypergraphql.org/query");
        Selector sel = new SelectorImpl(res, null, (Object) null);
        StmtIterator iterator = returnedModel.listStatements(sel);
        Set<Statement> statements = new HashSet<>();
        while (iterator.hasNext()) {
            statements.add(iterator.nextStatement());
        }

        for (Statement statement : statements) {
            returnedModel.remove(statement);
        }

        StmtIterator iterator2 = expectedModel.listStatements();
        while (iterator2.hasNext()) {
            Statement currentstatement = iterator2.next();
            if (!returnedModel.contains(currentstatement)) {
                System.out.println(currentstatement);
            }
        }

        assertTrue(expectedModel.isIsomorphicWith(returnedModel));
        server.stop();
    }
}
