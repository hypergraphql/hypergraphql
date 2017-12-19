package org.hypergraphql.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.jena.fuseki.embedded.FusekiServer;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.hypergraphql.Controller;
import org.hypergraphql.config.system.HGQLConfig;
import org.junit.jupiter.api.Test;

import javax.jws.soap.SOAPBinding;
import java.io.InputStream;

public class GeneralTest {



    //@Test
    public static void main (String[] args) {

       Model mainModel = ModelFactory.createDefaultModel();
       mainModel.read("src/test/resources/TestServices/dbpedia.ttl", "TTL");

       Model citiesModel = ModelFactory.createDefaultModel();
       citiesModel.read("src/test/resources/TestServices/cities.ttl", "TTL");
       Model expectedModel = ModelFactory.createDefaultModel();
       expectedModel.add(mainModel).add(citiesModel);
       expectedModel.write(System.out,"NTRIPLE");

        Dataset ds = DatasetFactory.createTxnMem() ;
        ds.setDefaultModel(citiesModel);
        FusekiServer server = FusekiServer.create()
                .add("/ds", ds)
                .build() ;
        server.start() ;



        HGQLConfig configext = new HGQLConfig("src/test/resources/TestServices/externalconfig.json");

        Controller controllerext = new Controller();
        controllerext.start(configext);

        HGQLConfig config = new HGQLConfig("src/test/resources/TestServices/mainconfig.json");

        Controller controller = new Controller();
        controller.start(config);



        ObjectMapper mapper = new ObjectMapper();

        ObjectNode bodyParam = mapper.createObjectNode();

        bodyParam.put("query", "{\n" +
                "  Person_GET {\n" +
                "    _id\n" +
                "    label\n" +
                "    birthPlace{\n" +
                "      _id\n" +
                "      label\n" +
                "    }\n" +
                "    \n" +
                "  }\n" +
                "  City_GET {\n" +
                "    _id\n" +
                "    label\n" +
                "  }\n" +
                "  \n" +
                "}");

        Model returnedModel = ModelFactory.createDefaultModel();

        try {
            HttpResponse<InputStream> response = Unirest.post("http://localhost:8080/graphql")
                    .header("Accept", "application/rdf+xml")
                    .body(bodyParam.toString())
                    .asBinary();

            System.out.println(response.getBody());
            returnedModel.read(response.getBody(), "RDF/XML");

        } catch (UnirestException e) {
            e.printStackTrace();
        }

        returnedModel.write(System.out, "NTRIPLE");









    }
}
