package org.hypergraphql.config.system;

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
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

import org.hypergraphql.datafetching.services.Service;
import org.hypergraphql.config.schema.FieldConfig;
import org.hypergraphql.config.schema.QueryFieldConfig;
import org.hypergraphql.config.schema.SchemElementConfig;
import org.hypergraphql.config.schema.TypeConfig;

import static graphql.Scalars.*;

/**
 * Created by szymon on 05/09/2017.
 */

public class HGQLConfig {

    private static HGQLConfig instance = null;
    private static String propertyFilepath = "properties.json";

    public static HGQLConfig getInstance() {
        if(instance == null) {
            instance = new HGQLConfig();
        }
        return instance;
    }

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

    private Map<String, String> JSONLD_VOC = new HashMap<String, String>() {{
    //    put("_context", "@context");
        put("_id", "@id");
    //    put("_value", "@value");
        put("_type", "@type");
     //   put("_language", "@language");
     //   put("_graph", "@graph");
    }};

    private String contextFile;
    private String schemaFile;
    private GraphqlConfig graphqlConfig;

    private JsonNode context;
    private ObjectNode mapping;
    private Map<String, GraphQLOutputType> outputTypes = new HashMap<>();
    private Map<String, TypeConfig> types;
    private Map<String, FieldConfig> fields;
    private Map<String, QueryFieldConfig> queryFields;

    private Map<String, Service> services;
    private TypeDefinitionRegistry registry;
    private GraphQLSchema schema;
    private GraphQL graphql;

    static Logger logger = Logger.getLogger(HGQLConfig.class);

    @JsonCreator
    private  HGQLConfig(@JsonProperty("contextFile") String contextFile,
                      @JsonProperty("schemaFile") String schemaFile,
                      @JsonProperty("graphql") GraphqlConfig graphql
    ) {
        this.contextFile = contextFile;
        this.schemaFile = schemaFile;
        this.graphqlConfig = graphql;
    }



    protected HGQLConfig() {

        ObjectMapper mapper = new ObjectMapper();

        try {
            HGQLConfig config = mapper.readValue(new File(propertyFilepath), HGQLConfig.class);

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

            generateServices(mapper);

            JsonNode predicatesJson = context.get("predicates");

            JsonNode typesJson = predicatesJson.get("types");

            typesJson.fieldNames().forEachRemaining(key -> {

                String id = typesJson.get(key).get("@id").asText();
                TypeConfig typeConfig = new TypeConfig(id);
                this.types.put(key, typeConfig);
            });

            JsonNode fieldsJson = predicatesJson.get("fields");

            fieldsJson.fieldNames().forEachRemaining(key -> {


                Service service = this.services.get(fieldsJson.get(key).get("service").asText());
                String id = fieldsJson.get(key).get("@id").asText();
                FieldConfig fieldConfig = new FieldConfig(id, service);
                this.fields.put(key, fieldConfig);

            });

            JsonNode queryFieldsJson = predicatesJson.get("queryFields");

            queryFieldsJson.fieldNames().forEachRemaining(key -> {

                Service service = this.services.get(queryFieldsJson.get(key).get("service").asText());
                QueryFieldConfig queryFieldConfig = new QueryFieldConfig(service);
                this.queryFields.put(key, queryFieldConfig);

            });

            SchemaParser schemaParser = new SchemaParser();
            this.registry = schemaParser.parse(new File(config.schemaFile));

            this.mapping = mapping(registry);

            this.schemaFile = config.schemaFile;
            this.contextFile = config.contextFile;
            this.graphqlConfig = config.graphqlConfig;

        } catch (IOException e) {
            logger.error(e);
        }


    }

    public void init() {
        GraphqlWiring wiring = new GraphqlWiring();
        this.schema = wiring.schema();
        this.graphql = GraphQL.newGraphQL(this.schema).build();
    }

    private void generateServices(ObjectMapper mapper) {

        String packageName = "org.hypergraphql.datafetching.services";

        context.get("services").elements().forEachRemaining(service -> {
                    try {
                        String type = service.get("@type").asText();

                        Class serviceType = Class.forName(packageName + "." + type);
                        Service serviceConfig = (Service) serviceType.getConstructors()[0].newInstance();

                        serviceConfig.setParameters(service);

                        this.services.put(serviceConfig.getId(), serviceConfig);
                    } catch (IllegalAccessException e) {
                        logger.error(e);
                    } catch (InstantiationException e) {
                        logger.error(e);
                    } catch (ClassNotFoundException e) {
                        logger.error(e);
                    } catch (InvocationTargetException e) {
                        logger.error(e);
                    }
                }
        );
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

    public Map<String, Service> services() { return this.services; }
    public Map<String, TypeConfig> types() { return this.types; }
    public Map<String, FieldConfig> fields() { return this.fields; }
    public Map<String, QueryFieldConfig> queryFields() { return this.queryFields; }

    public GraphQLSchema schema() {return schema; }

    public GraphQL graphql() {return graphql; }

    public Map<String, String> getJSONLD_VOC() { return JSONLD_VOC; }

}


