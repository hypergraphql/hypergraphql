package org.hypergraphql.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Service {

    @JsonCreator
    public Service(@JsonProperty("@type") String type,
                         @JsonProperty("@id") String id,
                         @JsonProperty("url") String url,
                         @JsonProperty("user") String user,
                         @JsonProperty("graph") String graph,
                         @JsonProperty("password") String password
    ) {
        this.type = type;
        this.id = id;
        this.url = url;
        this.graph = graph;
        this.user = user;
        this.password = password;

    }

    private String type;
    private String id;
    private String url;
    private String graph;
    private String user;
    private String password;

    public String type() { return this.type; }
    public String id() { return this.id; }
    public String url() { return this.url; }
    public String graph() { return this.graph; }
    public String user() { return this.user; }
    public String password() { return this.password; }

}