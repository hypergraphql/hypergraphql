package org.hypergraphql.config.system;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ServiceConfig {

    private String id;
    private String type;
    private String url;
    private String graph;
    private String user;
    private String password;
    private String filepath;
    private String filetype;

    @JsonCreator
    public ServiceConfig(@JsonProperty("id") String id,
                         @JsonProperty("type") String type,
                         @JsonProperty("url") String url,
                         @JsonProperty("graph") String graph,
                         @JsonProperty("user") String user,
                         @JsonProperty("password") String password,
                         @JsonProperty("filepath") String filepath,
                         @JsonProperty("filetype") String filetype
    ) {
        this.id = id;
        this.type = type;
        this.url = url;
        this.graph = graph;
        this.user = user;
        this.password = password;
        this.filepath = filepath;
        this.filetype = filetype;
    }

    public String getId() {
        return id;
    }
    public String getFilepath() {
        return filepath;
    }
    public String getFiletype() {
        return filetype;
    }
    public String getType() {
        return type;
    }
    public String getUrl() {
        return url;
    }
    public String getGraph() {
        return graph;
    }
    public String getUser() {
        return user;
    }
    public String getPassword() {
        return password;
    }

}
