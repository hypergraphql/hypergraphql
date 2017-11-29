package org.hypergraphql.datafetching.services;

public abstract class  SPARQLService extends Service {
    public SPARQLService(String type, String id, String url, String user, String graph, String password) {
        super(type, id, url, user, graph, password);
    }
}
