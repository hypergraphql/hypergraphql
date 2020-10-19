package org.hypergraphql.config.system;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class ServiceConfig {

    private final String id;
    private final String type;
    private final String url;
    private final String graph;
    private final String user;
    private final String password;
    private final String filepath;
    private final String filetype;

    @JsonCreator
    public ServiceConfig(@JsonProperty("id") final String id,
                         @JsonProperty("type") final String type,
                         @JsonProperty("url") final String url,
                         @JsonProperty("graph") final String graph,
                         @JsonProperty("user") final String user,
                         @JsonProperty("password") final String password,
                         @JsonProperty("filepath") final String filepath,
                         @JsonProperty("filetype") final String filetype
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
}
