import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Created by szymon on 05/09/2017.
 */

public class Config {

    public String contextFile;
    public String schemaFile;
    public GraphqlConfig graphql;

    public JsonNode context;
    public TypeDefinitionRegistry schema;

    @JsonCreator
    public Config(@JsonProperty("contextFile") String contextFile,
                  @JsonProperty("schemaFile") String schemaFile,
                  @JsonProperty("graphql") GraphqlConfig graphql
                  )
    {
        this.contextFile = contextFile;
        this.schemaFile = schemaFile;
        this.graphql = graphql;
    }

    public Config(String propertiesFile) {

        ObjectMapper mapper = new ObjectMapper();

        try {
            Config config = mapper.readValue(new File(propertiesFile), Config.class);

           if (config!=null) {
                try {
                    this.context = mapper.readTree(new File(config.contextFile));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            SchemaParser schemaParser = new SchemaParser();
            this.schema = schemaParser.parse(new File(config.schemaFile));


            this.schemaFile = config.schemaFile;
            this.contextFile = config.contextFile;
            this.graphql = config.graphql;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class GraphqlConfig {
    public Integer port;
    public String path;
    public String graphiql;

    @JsonCreator
    public GraphqlConfig(@JsonProperty("port") Integer port,
                        @JsonProperty("path") String path,
                        @JsonProperty("graphiql") String graphiql
    )
    {
        this.port = port;
        this.path = path;
        this.graphiql = graphiql;
    }

}

