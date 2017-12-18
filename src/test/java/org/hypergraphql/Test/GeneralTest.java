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

import java.io.InputStream;

public class GeneralTest {



    //@Test
    public static void main (String[] args) {

       Model mainModel = ModelFactory.createDefaultModel();
       mainModel.read("src/test/resources/TestServices/dbpedia.ttl", "TTL");

       Model citiesModel = ModelFactory.createDefaultModel();
       citiesModel.read("src/test/resources/TestServices/cities.ttl", "TTL");

        Dataset ds = DatasetFactory.createTxnMem() ;
        ds.setDefaultModel(citiesModel);
        FusekiServer server = FusekiServer.create()
                .add("/ds", ds)
                .build() ;
        server.start() ;

        QueryExecution qExe = QueryExecutionFactory.sparqlService( "http://localhost:3330/ds/sparql", "SELECT * WHERE { ?x ?y ?z } " );
        ResultSet results = qExe.execSelect();
        System.out.println("results = \n" );
        ResultSetFormatter.out(System.out,results);

        HGQLConfig configext = new HGQLConfig("src/test/resources/TestServices/externalconfig.json");

        Controller controllerext = new Controller();
        controllerext.start(configext);

        HGQLConfig config = new HGQLConfig("src/test/resources/TestServices/mainconfig.json");

        Controller controller = new Controller();
        controller.start(config);


//
//        ObjectMapper mapper = new ObjectMapper();
//
//        ObjectNode bodyParam = mapper.createObjectNode();
//
////        bodyParam.set("operationName", null);
////        bodyParam.set("variables", null);
//        bodyParam.put("query", graphQlQuery);
//
//        Model model = ModelFactory.createDefaultModel();
//
//        try {
//            HttpResponse<InputStream> response = Unirest.post(url)
//                    .header("Accept", "application/ttl")
//                    .body(bodyParam.toString())
//                    .asBinary();
//
//            model.read(response.getBody(), "RDF/XML");
//
//        } catch (UnirestException e) {
//            e.printStackTrace();
//        }



        server.stop() ;





    }
}
