package uk.co.semanticintegration.hypergraphql;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.sparql.ARQConstants;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.Symbol;
import org.apache.log4j.Logger;

/**
 * Created by szymon on 05/09/2017.
 */

public class Config {

    private String contextFile;
    private String schemaFile;
    private GraphqlConfig graphql;

    private JsonNode context;
    private ObjectNode mapping;
    private Map<String, Context> sparqlEndpointsContext;
    private TypeDefinitionRegistry schema;

    static Logger logger = Logger.getLogger(Config.class);



    @JsonCreator
    public Config(@JsonProperty("contextFile") String contextFile,
                  @JsonProperty("schemaFile") String schemaFile,
                  @JsonProperty("graphql") GraphqlConfig graphql
    ) {
        this.contextFile = contextFile;
        this.schemaFile = schemaFile;
        this.graphql = graphql;
    }

    public Config(String propertiesFile) {

        ObjectMapper mapper = new ObjectMapper();

        try {
            Config config = mapper.readValue(new File(propertiesFile), Config.class);

            if (config != null) {
                try {
                    this.context = mapper.readTree(new File(config.contextFile));
                } catch (IOException e) {
                    logger.error(e);
                }
            }


            String base = "http://jena.hpl.hp.com/Service#";
            Symbol queryAuthUser = ARQConstants.allocSymbol(base, "queryAuthUser");
            Symbol queryAuthPwd = ARQConstants.allocSymbol(base, "queryAuthPwd");

            Map<String, Context> serviceContexts = new HashMap<>();

            context.get("@endpoints").fieldNames().forEachRemaining(endpoint ->
                    {
                        Context servCont = new Context();
                        String uri = context.get("@endpoints").get(endpoint).get("@id").asText();
                        String user = context.get("@endpoints").get(endpoint).get("@user").asText();
                        String password = context.get("@endpoints").get(endpoint).get("@password").asText();
                        if (!user.equals("")) servCont.put(queryAuthUser, user);
                        if (!password.equals("")) servCont.put(queryAuthPwd, password);
                        serviceContexts.put(uri, servCont);
                    }
            );

            this.sparqlEndpointsContext = serviceContexts;

            SchemaParser schemaParser = new SchemaParser();
            this.schema = schemaParser.parse(new File(config.schemaFile));

            this.mapping = getMapping();




            this.schemaFile = config.schemaFile;
            this.contextFile = config.contextFile;
            this.graphql = config.graphql;

        } catch (IOException e) {
            logger.error(e);
        }
    }

    private ObjectNode getMapping() {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode result = mapper.createObjectNode();

        schema.types().forEach((typeName, typeDef) -> {

            ObjectNode typeObject = mapper.createObjectNode();

            if (containsPredicate(typeName)) typeObject.put("@id", predicateURI(typeName));

            try {
                JsonNode typeJson = mapper.readTree(new ObjectMapper().writeValueAsString(typeDef));

                typeJson.get("fieldDefinitions").elements().forEachRemaining(fieldDef ->
                {
                    ObjectNode fieldObject = mapper.createObjectNode();
                    String field = fieldDef.get("name").asText();
                    if (containsPredicate(getTarget(fieldDef))) {
                        fieldObject.put("targetURI", predicateURI(getTarget(fieldDef)));
                    } else {
                        fieldObject.put("targetScalar", getTarget(fieldDef));
                    }
                    if (containsPredicate(field)) fieldObject.put("uri", predicateURI(field));
                    if (containsPredicate(field)) fieldObject.put("graph", predicateGraph(field));
                    if (containsPredicate(field)) fieldObject.put("endpoint", predicateEndpoint(field));

                    typeObject.put(field, fieldObject);

                });

                result.put(typeName, typeObject);


            } catch (IOException e) {
                e.printStackTrace();
            }

            result.put(typeName, typeObject);
        });
        System.out.println("Printing mapping");
        System.out.println(result.toString());

        return result;
    }

    private String getTarget(JsonNode fieldDef) {
        JsonNode type = fieldDef.get("type");
        if (type.has("name")) {
            return type.get("name").asText();
        } else {
            return getTarget(type);
        }
    }


    public Boolean containsPredicate(String name) {

        return (context.get("@predicates").has(name));

    }

    public String predicateURI(String name) {

        return context.get("@predicates").get(name).get("@id").asText();

    }

    public String predicateGraph(String name) {

        String gName = context.get("@predicates").get(name).get("@namedGraph").asText();

        return context.get("@namedGraphs").get(gName).get("@id").asText();

    }

    public String predicateEndpoint(String name) {

        String gName = context.get("@predicates").get(name).get("@namedGraph").asText();

        String eName = context.get("@namedGraphs").get(gName).get("@endpoint").asText();

        return context.get("@endpoints").get(eName).get("@id").asText();

    }

    public GraphqlConfig graphql() {
        return graphql;
    }

    public TypeDefinitionRegistry schema() {
        return schema;
    }

    public Map<String, Context> sparqlEndpointsContext() {
        return sparqlEndpointsContext;
    }
}

class GraphqlConfig {

    private Integer port;
    private String path;
    private String graphiql;

    @JsonCreator
    public GraphqlConfig(@JsonProperty("port") Integer port,
                         @JsonProperty("path") String path,
                         @JsonProperty("graphiql") String graphiql
    ) {
        this.port = port;
        this.path = path;
        this.graphiql = graphiql;
    }

    public Integer port() {
        return port;
    }

    public String path() {
        return path;
    }

    public String graphiql() {
        return graphiql;
    }
}

