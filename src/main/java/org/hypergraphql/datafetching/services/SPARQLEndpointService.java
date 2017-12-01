package org.hypergraphql.datafetching.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.HttpAuthenticator;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.web.HttpOp;
import org.apache.jena.sparql.engine.http.Params;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.hypergraphql.datafetching.TreeExecutionResult;
import org.hypergraphql.query.converters.SPARQLServiceConverter;

import java.io.InputStream;
import java.net.URI;
import java.util.*;

public class SPARQLEndpointService extends SPARQLService {

    private String url;
    private String user;
    private String password;
    private int VALUES_SIZE_LIMIT = 100;


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    public SPARQLEndpointService() {

    }

    @Override
    public TreeExecutionResult executeQuery(JsonNode query, Set<String> input, String rootType, Set<String> markers) {

        Map<String, Set<String>> resultSet = new HashMap<>();

        for (String marker : markers) {
            resultSet.put(marker, new HashSet<>());
        }

        List<String> inputList = (List<String>) new ArrayList(input);

        do {

            Set<String> inputSubset = new HashSet<>();
            int i = 0;
            while (i < VALUES_SIZE_LIMIT && !inputList.isEmpty()) {
                inputSubset.add(inputList.get(0));
                inputList.remove(0);
                i++;
            }

            SPARQLServiceConverter converter = new SPARQLServiceConverter();
            String sparqlQuery = converter.getSelectQuery(query, inputSubset);

            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            Credentials credentials = new UsernamePasswordCredentials(this.user, this.password);
            credsProvider.setCredentials(AuthScope.ANY, credentials);
            HttpClient httpclient = HttpClients.custom()
                    .setDefaultCredentialsProvider(credsProvider)
                    .build();
            HttpOp.setDefaultHttpClient(httpclient);

            Query jenaQuery  = QueryFactory.create(sparqlQuery);
            QueryEngineHTTP qEngine = QueryExecutionFactory.createServiceRequest(this.url, jenaQuery);
            qEngine.setClient(httpclient);

            ResultSet results = qEngine.execSelect();

            while (results.hasNext()) {
                QuerySolution solution = results.nextSolution();

                System.out.println(solution.get("x_1"));
                Model model = getModelFromResults(solution);
            }

        } while (inputList.size()>VALUES_SIZE_LIMIT);



        //todo
    //    Model model = getModelFromResults(results);

        //todo : Szymon
        return null;
    }

    private Model getModelFromResults(QuerySolution results) {
        //todo


        return null;

    }

    @Override
    public void setParameters(JsonNode jsonnode) {

        super.setParameters(jsonnode);

        this.id = jsonnode.get("@id").asText();
        this.url = jsonnode.get("url").asText();
        this.user = jsonnode.get("user").asText();
        this.password = jsonnode.get("password").asText();

    }
}
