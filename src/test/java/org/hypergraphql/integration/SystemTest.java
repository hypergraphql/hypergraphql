package org.hypergraphql.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import lombok.val;
import org.apache.jena.fuseki.embedded.FusekiServer;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.SelectorImpl;
import org.hypergraphql.Controller;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.services.HGQLConfigService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemTest {

    private HGQLConfigService configService;

    @Test
    void integration_test() {

        val mainModel = ModelFactory.createDefaultModel();
        val dbpediaContentUrl = getClass().getClassLoader().getResource("test_services/dbpedia.ttl");
        if (dbpediaContentUrl != null) {
            mainModel.read(dbpediaContentUrl.toString(), "TTL");
        }

        val citiesModel = ModelFactory.createDefaultModel();
        val citiesContentUrl = getClass().getClassLoader().getResource("test_services/cities.ttl");
        if (citiesContentUrl != null) {
            citiesModel.read(citiesContentUrl.toString(), "TTL");
        }

        val expectedModel = ModelFactory.createDefaultModel();
        expectedModel.add(mainModel).add(citiesModel);

        val ds = DatasetFactory.createTxnMem();
        ds.setDefaultModel(citiesModel);
        val server = FusekiServer.create()
                .add("/ds", ds)
                .build()
                .start();

        val externalConfig = fromClasspathConfig("test_services/externalconfig.json");
        val externalController = new Controller();
        externalController.start(externalConfig);

        val config = fromClasspathConfig("test_services/mainconfig.json");

        val controller = new Controller();
        controller.start(config);

        val mapper = new ObjectMapper();
        val bodyParam = mapper.createObjectNode();

        bodyParam.put("query", "{\n"
                + "  Person_GET {\n"
                + "    _id\n"
                + "    label\n"
                + "    name\n"
                + "    birthPlace {\n"
                + "      _id\n"
                + "      label\n"
                + "    }\n"
                + "    \n"
                + "  }\n"
                + "  City_GET {\n"
                + "    _id\n"
                + "    label}\n"
                + "}");

        val returnedModel = ModelFactory.createDefaultModel();

        try {
            HttpResponse<InputStream> response = Unirest.post("http://localhost:8080/graphql")
                    .header("Accept", "application/rdf+xml")
                    .body(bodyParam.toString())
                    .asBinary();
            returnedModel.read(response.getBody(), "RDF/XML");
        } catch (UnirestException e) {
            e.printStackTrace();
        }

        val res = ResourceFactory.createResource("http://hypergraphql.org/query");
        val sel = new SelectorImpl(res, null, (Object) null);
        val iterator = returnedModel.listStatements(sel);
        final Set<Statement> statements = new HashSet<>();
        while (iterator.hasNext()) {
            statements.add(iterator.nextStatement());
        }

        for (Statement statement : statements) {
            returnedModel.remove(statement);
        }

//        StmtIterator iterator2 = expectedModel.listStatements();
//        while (iterator2.hasNext()) {
//            assertTrue(returnedModel.contains(iterator2.next()));
//        }

        assertTrue(expectedModel.isIsomorphicWith(returnedModel));
        externalController.stop();
        controller.stop();
        server.stop();
    }

    private HGQLConfig fromClasspathConfig(final String configPath) {

        if (configService == null) {
            configService = new HGQLConfigService();
        }

        val inputStream = getClass().getClassLoader().getResourceAsStream(configPath);
        return configService.loadHGQLConfig(configPath, inputStream, true);
    }
}
