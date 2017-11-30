package org.hypergraphql.datafetching.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.hypergraphql.datafetching.TreeExecutionResult;

import java.util.Set;

public class SPARQLEndpointService extends SPARQLService {

    private String url;
    private String user;
    private String password;


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


    @Override
    public TreeExecutionResult executeQuery(JsonNode query, Set<String> input) {

        //todo : Szymon
        return null;
    }
}
