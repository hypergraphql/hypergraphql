package org.hypergraphql;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.query.*;
import org.apache.jena.riot.web.HttpOp;
import org.apache.jena.sparql.ARQConstants.*;
import org.apache.jena.sparql.ARQConstants;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.Symbol;


import java.io.InputStream;


/**
 * Created by philipcoates on 2017-10-19T20:22
 */
class SparqlClientTest {
//
//    @integration
//    public void query_with_auth_11() {
//
//    }
//
//    @integration
//    public void unirestSparqlQuery() throws UnirestException {
//
////        String queryString = "SELECT * WHERE { ?x ?y ?z } LIMIT 10";
////        String url = "http://ec2-52-23-193-209.compute-1.amazonaws.com:5820/photobox/query";
////        String user = "admin";
////        String password = "admin";
////
////        HttpResponse<JsonNode> resp = Unirest.get(url)
////                .queryString("query", queryString)
////                .header("Accept", "application/sparql-results+json")
////                .basicAuth(user, password)
////                .asJson();
////
////        System.out.println("Unirest: " + resp.getStatus());
////        resp.getBody().getObject().getJSONObject("head").getJSONArray("vars").iterator().forEachRemaining(var -> System.out.println(var));
//
//    }
}