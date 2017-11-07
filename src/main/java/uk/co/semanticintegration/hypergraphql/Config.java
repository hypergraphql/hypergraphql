package uk.co.semanticintegration.hypergraphql;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.language.TypeDefinition;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
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

            this.mapping = mapping(schema);

            this.schemaFile = config.schemaFile;
            this.contextFile = config.contextFile;
            this.graphql = config.graphql;

        } catch (IOException e) {
            logger.error(e);
        }
    }

    private ObjectNode mapping(TypeDefinitionRegistry registry) throws IOException {

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring().build();
        GraphQLSchema baseSchema = schemaGenerator.makeExecutableSchema(registry, wiring);
        GraphQL graphQL = GraphQL.newGraphQL(baseSchema).build();

        String query = "{\n" +
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

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(query)
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
                if (containsPredicate(typeName)) {
                    typeObject.put("uri", predicateURI(typeName));
                    typeObject.put("graph", predicateGraph(typeName));
                    typeObject.put("endpoint", predicateGraph(typeName));
                }

                if (type.has("fields")) {
                    JsonNode oldFields = type.get("fields");
                    ObjectNode fieldsObject = mapper.createObjectNode();

                    oldFields.elements().forEachRemaining(field -> {
                        String fieldName = field.get("name").asText();

                        ObjectNode fieldObject = mapper.createObjectNode();

                       // ObjectNode fieldObject = (ObjectNode) field;

                        if (containsPredicate(fieldName)) {
                            fieldObject.put("uri", predicateURI(fieldName));
                            fieldObject.put("graph", predicateGraph(fieldName));
                            fieldObject.put("endpoint", predicateGraph(fieldName));
                        }

                        Map <String, Object> targetInfoMap = new HashMap<>();
                        targetInfoMap.put("name", null);
                        targetInfoMap.put("kind", null);
                        targetInfoMap.put("inList", false);

                        Map<String, Object> targetTypeInfo = getTargetTypeInfo(field.get("type"), targetInfoMap);

                        String targetTypeName = (String) targetTypeInfo.get("name");
                        if (containsPredicate(targetTypeName)) {
                            fieldObject.put("targetUri", predicateURI(targetTypeName));
                            fieldObject.put("targetGraph", predicateGraph(targetTypeName));
                            fieldObject.put("targetEndpoint", predicateGraph(targetTypeName));
                        }
                        fieldObject.put("targetName", targetTypeName);

                        Boolean targetIsList = (Boolean) targetTypeInfo.get("inList");
                        fieldObject.put("targetInList", targetIsList);

                        String targetTypeKind = (String) targetTypeInfo.get("kind");
                        fieldObject.put("targetKind", targetTypeKind);

                        fieldsObject.put(fieldName, fieldObject);
                    });

                    typeObject.put("fields", fieldsObject);

                }


                result.put(typeName, typeObject);
            }

        });

        return result;
    }

    private Map<String,Object> getTargetTypeInfo(JsonNode type, Map<String, Object> targetInfoMap) {

        if (!type.get("name").isNull()) {
            targetInfoMap.put("name", type.get("name").asText());
            targetInfoMap.put("kind", type.get("kind").asText());
        }

        if (type.get("kind").asText().equals("LIST")) {

                targetInfoMap.put("inList", true);
        }

        if (type.has("ofType") && !type.get("ofType").isNull()) {
            return getTargetTypeInfo(type.get("ofType"), targetInfoMap);
        } else {
            return targetInfoMap;
        }
    }

    public ObjectNode mapping() {
        return this.mapping;
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

