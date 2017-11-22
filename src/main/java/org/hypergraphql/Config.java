package org.hypergraphql;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.language.TypeDefinition;
import graphql.schema.*;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

import javax.xml.ws.Service;

import static graphql.Scalars.*;

/**
 * Created by szymon on 05/09/2017.
 */

public class Config {

    private final String SCHEMA_QUERY =
            "{\n" +
            "  __schema {\n" +
            "    types {\n" +
            "      name\n" +
            "      kind\n" +
            "      fields {\n" +
            "        name\n" +
            "        type {\n" +
            "          kind\n" +
            "          name\n" +
            "          ofType {\n" +
            "            kind\n" +
            "            name\n" +
            "            ofType {\n" +
            "              kind\n" +
            "              name\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}\n";

    private String contextFile;
    private String schemaFile;
    private GraphqlConfig graphqlConfig;

    private JsonNode context;
    private ObjectNode mapping;
    private Map<String, GraphQLOutputType> outputTypes = new HashMap<>();
    private Map<String, PredicateConfig> types;
    private Map<String, PredicateConfig> fields;
    private Map<String, PredicateConfig> queryFields;
    private Map<String, ServiceConfig> services;
    private TypeDefinitionRegistry registry;
    private GraphQLSchema schema;
    private GraphQL graphql;

    static Logger logger = Logger.getLogger(Config.class);

    @JsonCreator
    public Config(@JsonProperty("contextFile") String contextFile,
                  @JsonProperty("schemaFile") String schemaFile,
                  @JsonProperty("graphql") GraphqlConfig graphql
    ) {
        this.contextFile = contextFile;
        this.schemaFile = schemaFile;
        this.graphqlConfig = graphql;
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

            this.services = new HashMap<>();
            this.types = new HashMap<>();
            this.queryFields = new HashMap<>();
            this.fields = new HashMap<>();

            JsonNode servicesJson = context.get("services");

            servicesJson.fieldNames().forEachRemaining(serviceKey -> {
                        try {
                            ServiceConfig service = mapper.readValue(servicesJson.get(serviceKey).toString(), ServiceConfig.class);
                            this.services.put(serviceKey, service);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
            );

            JsonNode predicatesJson = context.get("predicates");

            JsonNode typesJson = predicatesJson.get("types");

            typesJson.fieldNames().forEachRemaining(key ->
                    this.types.put(key, new PredicateConfig(typesJson.get(key), this.services))
                );

            JsonNode fieldsJson = predicatesJson.get("fields");

            fieldsJson.fieldNames().forEachRemaining(key ->
                    this.fields.put(key, new PredicateConfig(fieldsJson.get(key), this.services))
                );

            JsonNode queryFieldsJson = predicatesJson.get("queryFields");

            queryFieldsJson.fieldNames().forEachRemaining(key ->
                        this.queryFields.put(key, new PredicateConfig(queryFieldsJson.get(key), this.services))
                );

            SchemaParser schemaParser = new SchemaParser();
            this.registry = schemaParser.parse(new File(config.schemaFile));

            this.mapping = mapping(registry);

            this.schemaFile = config.schemaFile;
            this.contextFile = config.contextFile;
            this.graphqlConfig = config.graphqlConfig;

        } catch (IOException e) {
            logger.error(e);
        }

        GraphqlWiring wiring = new GraphqlWiring(this);
        this.schema = wiring.schema();
        this.graphql = GraphQL.newGraphQL(this.schema).build();
    }

    private ObjectNode mapping(TypeDefinitionRegistry registry) throws IOException {

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring().build();
        GraphQLSchema baseSchema = schemaGenerator.makeExecutableSchema(registry, wiring);
        GraphQL graphQL = GraphQL.newGraphQL(baseSchema).build();

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(SCHEMA_QUERY)
                .build();

        ExecutionResult qlResult = graphQL.execute(executionInput);

        Map<String, TypeDefinition> registered = registry.types();

        ObjectMapper mapper = new ObjectMapper();

        JsonNode introspectionResult = mapper.readTree(new ObjectMapper().writeValueAsString(qlResult.getData())).get("__schema").get("types");

        ObjectNode result = mapper.createObjectNode();

        introspectionResult.elements().forEachRemaining(type -> {

            String typeName = type.get("name").asText();

            if (registered.containsKey(typeName)) {
                ObjectNode typeObject = mapper.createObjectNode();
                typeObject.put("name", typeName);
                if (type.has("fields")) {
                    JsonNode oldFields = type.get("fields");
                    ObjectNode fieldsObject = mapper.createObjectNode();

                    oldFields.elements().forEachRemaining(field -> {

                        String fieldName = field.get("name").asText();

                        ObjectNode fieldObject = mapper.createObjectNode();

                        fieldObject.put("name", fieldName);

                        Map <String, Object> targetInfoMap = new HashMap<>();
                        targetInfoMap.put("name", null);
                        targetInfoMap.put("kind", null);
                        targetInfoMap.put("inList", false);
                        targetInfoMap.put("output", null);

                        Map<String, Object> targetTypeInfo = getTargetTypeInfo(field.get("type"), targetInfoMap);

                        String targetTypeName = (String) targetTypeInfo.get("name");
                        fieldObject.put("targetName", targetTypeName);

                        Boolean targetIsList = (Boolean) targetTypeInfo.get("inList");
                        fieldObject.put("targetInList", targetIsList);

                        String targetTypeKind = (String) targetTypeInfo.get("kind");
                        fieldObject.put("targetKind", targetTypeKind);

                        GraphQLOutputType outputType = (GraphQLOutputType) targetTypeInfo.get("output");
                        String id = UUID.randomUUID().toString();
                        outputTypes.put(id, outputType);
                        fieldObject.put("targetTypeId", id);

                        fieldsObject.put(fieldName, fieldObject);

                    });

                    typeObject.put("fields", fieldsObject);

                }

                result.put(typeName, typeObject);
            }

        });

        logger.debug(result.toString());

        return result;
    }

    private Map<String, Object> getTargetTypeInfo(JsonNode type, Map<String, Object> targetInfoMap) {

        if (!type.get("name").isNull()) {
            targetInfoMap.put("name", type.get("name").asText());
            targetInfoMap.put("kind", type.get("kind").asText());
        }

        if (type.get("kind").asText().equals("LIST")) {

            targetInfoMap.put("inList", true);
        }

        if (type.has("ofType") && !type.get("ofType").isNull()) {

            Map<String, Object> nestedInfo = getTargetTypeInfo(type.get("ofType"), targetInfoMap);
            GraphQLOutputType nestedOutputType = (GraphQLOutputType) nestedInfo.get("output");

            if (type.get("kind").asText().equals("LIST")) {
                nestedInfo.put("output", new GraphQLList(nestedOutputType));
            }

            if (type.get("kind").asText().equals("NON_NULL")) {
                nestedInfo.put("output", new GraphQLNonNull(nestedOutputType));
            }

            return nestedInfo;

        } else {

            String kind = type.get("kind").asText();
            String name = type.get("name").asText();

            if (kind.equals("OBJECT")) {
                targetInfoMap.put("output", new GraphQLTypeReference(name));
            }

            if (kind.equals("SCALAR")) {

                switch (name) {
                    case "String": {
                        targetInfoMap.put("output", GraphQLString);
                        break;
                    }
                    case "ID": {
                        targetInfoMap.put("output", GraphQLID);
                        break;
                    }
                    case "Int": {
                        targetInfoMap.put("output", GraphQLInt);
                        break;
                    }
                    case "Boolean": {
                        targetInfoMap.put("output", GraphQLBoolean);
                        break;
                    }
                }
            }

            return targetInfoMap;
        }
    }

    public ObjectNode mapping() {
        return this.mapping;
    }

    public GraphQLOutputType outputType(String id) {
        return outputTypes.get(id);
    }



    public GraphqlConfig graphqlConfig() {
        return graphqlConfig;
    }

    public Map<String, ServiceConfig> services() { return this.services; }
    public Map<String, PredicateConfig> types() { return this.types; }
    public Map<String, PredicateConfig> fields() { return this.fields; }
    public Map<String, PredicateConfig> queryFields() { return this.queryFields; }

    public GraphQLSchema schema() {return schema; }

    public GraphQL graphql() {return graphql; }

}

class PredicateConfig {
    private String id;
    private ServiceConfig service;

    public PredicateConfig(JsonNode predicateJson, Map<String, ServiceConfig> services) {

        if (predicateJson.has("@id")) this.id = predicateJson.get("@id").toString();
        if (predicateJson.has("service")) this.service = services.get(predicateJson.get("service").asText());

    }

    public String id() { return this.id; }
    public ServiceConfig service() { return this.service; }
}


class ServiceConfig {

    @JsonCreator
    public ServiceConfig(@JsonProperty("@type") String type,
                         @JsonProperty("url") String url,
                         @JsonProperty("user") String user,
                         @JsonProperty("graph") String graph,
                         @JsonProperty("password") String password
    ) {
        this.type = type;
        this.url = url;
        this.graph = graph;
        this.user = user;
        this.password = password;

    }

    private String type;
    private String url;
    private String graph;
    private String user;
    private String password;

    public String type() { return this.type; }
    public String url() { return this.url; }
    public String graph() { return this.graph; }
    public String user() { return this.user; }
    public String password() { return this.password; }

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

