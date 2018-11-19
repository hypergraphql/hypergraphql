package org.hypergraphql.datamodel;

import graphql.language.Directive;
import graphql.language.FieldDefinition;
import graphql.language.ListType;
import graphql.language.Node;
import graphql.language.NonNullType;
import graphql.language.StringValue;
import graphql.language.Type;
import graphql.language.TypeDefinition;
import graphql.language.TypeName;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.hypergraphql.config.schema.FieldConfig;
import org.hypergraphql.config.schema.FieldOfTypeConfig;
import org.hypergraphql.config.schema.QueryFieldConfig;
import org.hypergraphql.config.schema.TypeConfig;
import org.hypergraphql.datafetching.services.Service;
import org.hypergraphql.exception.HGQLConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_Boolean;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_FIELD;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_HAS_FIELD;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_HAS_ID;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_HAS_NAME;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_HAS_SERVICE;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_HREF;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_ID;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_Int;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_LIST_TYPE;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_NON_NULL_TYPE;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_OBJECT_TYPE;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_OF_TYPE;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_OUTPUT_TYPE;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_QUERY_FIELD;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_QUERY_GET_BY_ID_FIELD;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_QUERY_GET_FIELD;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_QUERY_TYPE;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_SCALAR_TYPE;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_SCHEMA;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_SCHEMA_NAMESPACE;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_SERVICE;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_SERVICE_NAMESPACE;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_STRING;
import static org.hypergraphql.config.schema.HGQLVocabulary.RDF_TYPE;
import static org.hypergraphql.config.schema.HGQLVocabulary.SCALAR_TYPES;
import static org.hypergraphql.config.schema.HGQLVocabulary.SCALAR_TYPES_TO_GRAPHQL_OUTPUT;


public class HGQLSchema {

    private final static Logger LOGGER = LoggerFactory.getLogger(HGQLSchema.class);

    private String schemaUri;
    private String schemaNamespace;


    public Map<String, TypeConfig> getTypes() {
        return types;
    }

    public Map<String, FieldConfig> getFields() {
        return fields;
    }

    public Map<String, QueryFieldConfig> getQueryFields() {
        return queryFields;
    }

    public String getRdfSchemaOutput(String format) {
        return rdfSchema.getDataOutput(format);
    }

    private Map<String, TypeConfig> types;
    private Map<String, FieldConfig> fields;
    private Map<String, QueryFieldConfig> queryFields;

    private ModelContainer rdfSchema = new ModelContainer(ModelFactory.createDefaultModel());

    public HGQLSchema(TypeDefinitionRegistry registry, String schemaName, Map<String, Service> services)
            throws HGQLConfigurationException {

        schemaUri = HGQL_SCHEMA_NAMESPACE + schemaName;
        schemaNamespace = schemaUri + "/";

        rdfSchema.insertObjectTriple(schemaUri, RDF_TYPE, HGQL_SCHEMA);
        rdfSchema.insertObjectTriple(schemaNamespace + "query", RDF_TYPE, HGQL_QUERY_TYPE);
        rdfSchema.insertStringLiteralTriple(schemaNamespace + "query", HGQL_HAS_NAME, "Query");
        rdfSchema.insertObjectTriple(HGQL_STRING, RDF_TYPE, HGQL_SCALAR_TYPE);
        rdfSchema.insertStringLiteralTriple(HGQL_STRING, HGQL_HAS_NAME, "String");
        rdfSchema.insertObjectTriple(HGQL_Int, RDF_TYPE, HGQL_SCALAR_TYPE);
        rdfSchema.insertStringLiteralTriple(HGQL_Int, HGQL_HAS_NAME, "Int");
        rdfSchema.insertObjectTriple(HGQL_Boolean, RDF_TYPE, HGQL_SCALAR_TYPE);
        rdfSchema.insertStringLiteralTriple(HGQL_Boolean, HGQL_HAS_NAME, "Boolean");
        rdfSchema.insertObjectTriple(HGQL_ID, RDF_TYPE, HGQL_SCALAR_TYPE);
        rdfSchema.insertStringLiteralTriple(HGQL_ID, HGQL_HAS_NAME, "ID");

        Map<String, TypeDefinition> types = registry.types();

        TypeDefinition context = types.get("__Context");

        if (context == null) {
            HGQLConfigurationException e =
                    new HGQLConfigurationException("The provided GraphQL schema IDL specification is missing the obligatory __Context type (see specs at http://hypergraphql.org).");
            LOGGER.error("Context not set!", e);
            throw(e);
        }

        List<Node> children = context.getChildren();

        Map<String, String> contextMap = new HashMap<>();

        children.forEach(node -> {
            FieldDefinition field = ((FieldDefinition) node);
            String iri = ((StringValue) field.getDirective("href").getArgument("iri").getValue()).getValue();
            contextMap.put(field.getName(), iri);
        });

        Set<String> typeNames = types.keySet();
        typeNames.remove("__Context");

        Set<String> serviceIds = services.keySet();

        serviceIds.forEach(serviceId -> {
            String serviceURI = HGQL_SERVICE_NAMESPACE + serviceId;
            rdfSchema.insertObjectTriple(serviceURI, RDF_TYPE, HGQL_SERVICE);
            rdfSchema.insertStringLiteralTriple(serviceURI, HGQL_HAS_ID, serviceId);
        });


        typeNames.forEach(typeName -> {

            String typeUri = schemaNamespace + typeName;
            rdfSchema.insertStringLiteralTriple(typeUri, HGQL_HAS_NAME, typeName);
            rdfSchema.insertObjectTriple(typeUri, HGQL_HREF, contextMap.get(typeName));
            rdfSchema.insertObjectTriple(typeUri, RDF_TYPE, HGQL_OBJECT_TYPE);


            TypeDefinition type = types.get(typeName);
            final List<Directive> directives = type.getDirectives();

            directives.forEach(dir -> {
                if (dir.getName().equals("service")) {
                    String getQueryUri = typeUri + "_GET";
                    String getByIdQueryUri = typeUri + "_GET_BY_ID";

                    rdfSchema.insertObjectTriple(getQueryUri, RDF_TYPE, HGQL_QUERY_FIELD);
                    rdfSchema.insertObjectTriple(getQueryUri, RDF_TYPE, HGQL_QUERY_GET_FIELD);
                    rdfSchema.insertObjectTriple(schemaNamespace + "query", HGQL_HAS_FIELD, getQueryUri);
                    rdfSchema.insertStringLiteralTriple(getQueryUri, HGQL_HAS_NAME, typeName + "_GET");
                    rdfSchema.insertObjectTriple(getByIdQueryUri, RDF_TYPE, HGQL_QUERY_FIELD);
                    rdfSchema.insertObjectTriple(getByIdQueryUri, RDF_TYPE, HGQL_QUERY_GET_BY_ID_FIELD);
                    rdfSchema.insertObjectTriple(schemaNamespace + "query", HGQL_HAS_FIELD, getByIdQueryUri);
                    rdfSchema.insertStringLiteralTriple(getByIdQueryUri, HGQL_HAS_NAME, typeName + "_GET_BY_ID");

                    String outputListTypeURI = schemaNamespace + UUID.randomUUID();

                    rdfSchema.insertObjectTriple(outputListTypeURI, RDF_TYPE, HGQL_LIST_TYPE);
                    rdfSchema.insertObjectTriple(outputListTypeURI, HGQL_OF_TYPE, typeUri);

                    rdfSchema.insertObjectTriple(getQueryUri, HGQL_OUTPUT_TYPE, outputListTypeURI);
                    rdfSchema.insertObjectTriple(getByIdQueryUri, HGQL_OUTPUT_TYPE, outputListTypeURI);
                    String serviceId = ((StringValue) dir.getArgument("id").getValue()).getValue();

                    String serviceURI = HGQL_SERVICE_NAMESPACE + serviceId;
                    rdfSchema.insertObjectTriple(getQueryUri, HGQL_HAS_SERVICE, serviceURI);
                    rdfSchema.insertObjectTriple(getByIdQueryUri, HGQL_HAS_SERVICE, serviceURI);
                }
            });

            List<Node> typeChildren = type.getChildren();

            typeChildren.forEach(node -> {
                if (node.getClass().getSimpleName().equals("FieldDefinition")) {
                    FieldDefinition field = (FieldDefinition) node;
                    String fieldURI = schemaNamespace + typeName + "/" + field.getName();

                    rdfSchema.insertStringLiteralTriple(fieldURI, HGQL_HAS_NAME, field.getName());
                    rdfSchema.insertObjectTriple(fieldURI, HGQL_HREF, contextMap.get(field.getName()));

                    rdfSchema.insertObjectTriple(fieldURI, RDF_TYPE, HGQL_FIELD);
                    rdfSchema.insertObjectTriple(typeUri, HGQL_HAS_FIELD, fieldURI);

                    String serviceId = ((StringValue) field.getDirective("service").getArgument("id").getValue()).getValue();
                    String serviceURI = HGQL_SERVICE_NAMESPACE + serviceId;
                    rdfSchema.insertObjectTriple(fieldURI, HGQL_HAS_SERVICE, serviceURI);
                    rdfSchema.insertObjectTriple(serviceURI, RDF_TYPE, HGQL_SERVICE);

                    String outputTypeUri = getOutputType(field.getType());
                    rdfSchema.insertObjectTriple(fieldURI, HGQL_OUTPUT_TYPE, outputTypeUri);

                }
            });
        });

        generateConfigs(services);

    }

    private void generateConfigs(Map<String, Service> services) {
        this.types = new HashMap<>();
        this.fields = new HashMap<>();
        this.queryFields = new HashMap<>();

        List<RDFNode> fieldNodes = rdfSchema.getSubjectsOfObjectProperty(RDF_TYPE, HGQL_FIELD);

        fieldNodes.forEach(rdfNode -> {

            String name = rdfSchema.getValueOfDataProperty(rdfNode, HGQL_HAS_NAME);
            RDFNode href = rdfSchema.getValueOfObjectProperty(rdfNode, HGQL_HREF);
            RDFNode serviceNode = rdfSchema.getValueOfObjectProperty(rdfNode, HGQL_HAS_SERVICE);
            String serviceId = rdfSchema.getValueOfDataProperty(serviceNode, HGQL_HAS_ID);

            FieldConfig fieldConfig = new FieldConfig(href.asResource().getURI());
            fields.put(name, fieldConfig);
        });

        List<RDFNode> queryFieldNodes = rdfSchema.getSubjectsOfObjectProperty(RDF_TYPE, HGQL_QUERY_FIELD);
        List<RDFNode> queryGetFieldNodes = rdfSchema.getSubjectsOfObjectProperty(RDF_TYPE, HGQL_QUERY_GET_FIELD);

        queryFieldNodes.forEach(node -> {

            String name = rdfSchema.getValueOfDataProperty(node, HGQL_HAS_NAME);
            RDFNode serviceNode = rdfSchema.getValueOfObjectProperty(node, HGQL_HAS_SERVICE);
            String serviceId = rdfSchema.getValueOfDataProperty(serviceNode, HGQL_HAS_ID);

            String type = (queryGetFieldNodes.contains(node)) ? HGQL_QUERY_GET_FIELD : HGQL_QUERY_GET_BY_ID_FIELD;
            QueryFieldConfig fieldConfig = new QueryFieldConfig(services.get(serviceId), type);
            queryFields.put(name, fieldConfig);
        });

        List<RDFNode> typeNodes = rdfSchema.getSubjectsOfObjectProperty(RDF_TYPE, HGQL_OBJECT_TYPE);
        typeNodes.addAll(rdfSchema.getSubjectsOfObjectProperty(RDF_TYPE, HGQL_QUERY_TYPE));

        typeNodes.forEach(rdfNode -> {
            String typeName = rdfSchema.getValueOfDataProperty(rdfNode, HGQL_HAS_NAME);
            RDFNode typeHref = rdfSchema.getValueOfObjectProperty(rdfNode, HGQL_HREF);
            String typeURI = (typeHref!=null) ? typeHref.asResource().getURI() : null;

            List<RDFNode> fieldsOfType = rdfSchema.getValuesOfObjectProperty(rdfNode, HGQL_HAS_FIELD);
            Map<String, FieldOfTypeConfig> fields = new HashMap<>();

            fieldsOfType.forEach(field -> {

                String fieldOfTypeName = rdfSchema.getValueOfDataProperty(field, HGQL_HAS_NAME);
                RDFNode href = rdfSchema.getValueOfObjectProperty(field, HGQL_HREF);
                String hrefURI = (href!=null) ? href.asResource().getURI() : null;
                RDFNode serviceNode = rdfSchema.getValueOfObjectProperty(field, HGQL_HAS_SERVICE);
                String serviceId = rdfSchema.getValueOfDataProperty(serviceNode, HGQL_HAS_ID);
                RDFNode outputTypeNode = rdfSchema.getValueOfObjectProperty(field, HGQL_OUTPUT_TYPE);
                GraphQLOutputType graphqlOutputType = getGraphQLOutputType(outputTypeNode);
                Boolean isList = getIsList(outputTypeNode);
                String targetTypeName = getTargetTypeName(outputTypeNode);

                FieldOfTypeConfig fieldOfTypeConfig = new FieldOfTypeConfig(fieldOfTypeName, hrefURI, services.get(serviceId), graphqlOutputType, isList, targetTypeName);
                fields.put(fieldOfTypeName, fieldOfTypeConfig);

            });

            TypeConfig typeConfig = new TypeConfig(typeName, typeURI, fields);

            this.types.put(typeName, typeConfig);

        });
    }

    private String getTargetTypeName(RDFNode outputTypeNode) {
        String typeName = rdfSchema.getValueOfDataProperty(outputTypeNode, HGQL_HAS_NAME);
        if (typeName!=null) {
            return typeName;
        } else {
            RDFNode childOutputNode = rdfSchema.getValueOfObjectProperty(outputTypeNode, HGQL_OF_TYPE);
            return getTargetTypeName(childOutputNode);
        }
    }

    private Boolean getIsList(RDFNode outputTypeNode) {
        RDFNode outputNode = rdfSchema.getValueOfObjectProperty(outputTypeNode, RDF_TYPE);
        String typeURI = outputNode.asResource().getURI();
        if (typeURI.equals(HGQL_LIST_TYPE)) { return true; }
        else {
            RDFNode childOutputNode = rdfSchema.getValueOfObjectProperty(outputTypeNode, HGQL_OF_TYPE);
            if (childOutputNode!=null) { return getIsList(childOutputNode); }
            else {
                return false;
            }
        }
    }

    private GraphQLOutputType getGraphQLOutputType(RDFNode outputTypeNode) {
        RDFNode outputNode = rdfSchema.getValueOfObjectProperty(outputTypeNode, RDF_TYPE);
        String typeURI = outputNode.asResource().getURI();
        if (typeURI.equals(HGQL_SCALAR_TYPE)) {
            return SCALAR_TYPES_TO_GRAPHQL_OUTPUT.get(outputTypeNode.asResource().getURI());
        }
        if (typeURI.equals(HGQL_OBJECT_TYPE)) {
            String typeName = rdfSchema.getValueOfDataProperty(outputTypeNode, HGQL_HAS_NAME);
            return new GraphQLTypeReference(typeName);
        }
        if (typeURI.equals(HGQL_LIST_TYPE)) {
            RDFNode childOutputNode = rdfSchema.getValueOfObjectProperty(outputTypeNode, HGQL_OF_TYPE);
            return new GraphQLList(getGraphQLOutputType(childOutputNode));
        }
        if (typeURI.equals(HGQL_NON_NULL_TYPE)) {
            RDFNode childOutputNode = rdfSchema.getValueOfObjectProperty(outputTypeNode, HGQL_OF_TYPE);
            return new GraphQLNonNull(getGraphQLOutputType(childOutputNode));
        }
        return null;
    }


    private String getOutputType(Type type) {

        if (type.getClass() == TypeName.class) {
            TypeName castType = (TypeName) type;
            String name = castType.getName();

            if (SCALAR_TYPES.containsKey(name)) {
                return SCALAR_TYPES.get(name);
            } else return schemaNamespace + name;
        }

        String dummyNode = schemaNamespace + UUID.randomUUID();

        if (type.getClass() == ListType.class) {
            ListType castType = (ListType) type;
            String subTypeUri = getOutputType(castType.getType());
            rdfSchema.insertObjectTriple(dummyNode, RDF_TYPE, HGQL_LIST_TYPE);
            rdfSchema.insertObjectTriple(dummyNode, HGQL_OF_TYPE, subTypeUri);
        }

        if (type.getClass() == NonNullType.class) {
            NonNullType castType = (NonNullType) type;
            String subTypeUri = getOutputType(castType.getType());
            rdfSchema.insertObjectTriple(dummyNode, RDF_TYPE, HGQL_NON_NULL_TYPE);
            rdfSchema.insertObjectTriple(dummyNode, HGQL_OF_TYPE, subTypeUri);
        }

        return dummyNode;
    }

    public String getSchemaUri() {
        return schemaUri;
    }

    public String getSchemaNamespace() {
        return schemaNamespace;
    }
}
