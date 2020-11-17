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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.hypergraphql.config.schema.FieldConfig;
import org.hypergraphql.config.schema.FieldOfTypeConfig;
import org.hypergraphql.config.schema.QueryFieldConfig;
import org.hypergraphql.config.schema.TypeConfig;
import org.hypergraphql.datafetching.services.Service;
import org.hypergraphql.exception.HGQLConfigurationException;

import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_BOOLEAN;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_FIELD;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_HAS_FIELD;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_HAS_ID;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_HAS_NAME;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_HAS_SERVICE;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_HREF;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_ID;
import static org.hypergraphql.config.schema.HGQLVocabulary.HGQL_INT;
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

@Slf4j
public class HGQLSchema {

    private final String schemaUri;
    private final String schemaNamespace;

    private Map<String, TypeConfig> types;
    private Map<String, FieldConfig> fields;
    private Map<String, QueryFieldConfig> queryFields;

    private final ModelContainer rdfSchema = new ModelContainer(ModelFactory.createDefaultModel());

    public HGQLSchema(final TypeDefinitionRegistry registry,
                      final String schemaName,
                      final Map<String, Service> services)
            throws HGQLConfigurationException {

        schemaUri = HGQL_SCHEMA_NAMESPACE + schemaName;
        schemaNamespace = schemaUri + "/";

        populateHGQLRDFSchemaBaseTypes();

        final Map<String, TypeDefinition> registryTypes = registry.types();
        val context = registryTypes.get("__Context");

        if (context == null) {
            val e =
                    new HGQLConfigurationException("The provided GraphQL schema IDL specification is missing the"
                            + "obligatory __Context type (see specs at http://hypergraphql.org).");
            log.error("Context not set!", e);
            throw e;
        }
        final Map<String, String> contextMap = generateContextMap(context);

        populateServices(services);
        populateTypes(registryTypes, contextMap);
        generateConfigs(services);

    }

    public Map<String, TypeConfig> getTypes() {
        return types;
    }

    public Map<String, FieldConfig> getFields() {
        return fields;
    }

    public Map<String, QueryFieldConfig> getQueryFields() {
        return queryFields;
    }

    public String getRdfSchemaOutput(final String format) {
        return rdfSchema.getDataOutput(format);
    }

    private void generateConfigs(final Map<String, Service> services) { // TODO - Extract class
        this.types = new HashMap<>();
        this.fields = new HashMap<>();
        this.queryFields = new HashMap<>();

        final List<RDFNode> fieldNodes = rdfSchema.getSubjectsOfObjectProperty(RDF_TYPE, HGQL_FIELD);

        fieldNodes.forEach(rdfNode -> {

            val name = rdfSchema.getValueOfDataProperty(rdfNode, HGQL_HAS_NAME);
            val href = rdfSchema.getValueOfObjectProperty(rdfNode, HGQL_HREF);
            val serviceNode = rdfSchema.getValueOfObjectProperty(rdfNode, HGQL_HAS_SERVICE);

            val fieldConfig = new FieldConfig(href.asResource().getURI());
            fields.put(name, fieldConfig);
        });

        final List<RDFNode> queryFieldNodes = rdfSchema.getSubjectsOfObjectProperty(RDF_TYPE, HGQL_QUERY_FIELD);
        final List<RDFNode> queryGetFieldNodes = rdfSchema.getSubjectsOfObjectProperty(RDF_TYPE, HGQL_QUERY_GET_FIELD);

        queryFieldNodes.forEach(node -> {

            val name = rdfSchema.getValueOfDataProperty(node, HGQL_HAS_NAME);
            val serviceNode = rdfSchema.getValueOfObjectProperty(node, HGQL_HAS_SERVICE);
            val serviceId = rdfSchema.getValueOfDataProperty(serviceNode, HGQL_HAS_ID);

            val type = (queryGetFieldNodes.contains(node)) ? HGQL_QUERY_GET_FIELD : HGQL_QUERY_GET_BY_ID_FIELD;
            val fieldConfig = new QueryFieldConfig(services.get(serviceId), type);
            queryFields.put(name, fieldConfig);
        });

        final List<RDFNode> typeNodes = rdfSchema.getSubjectsOfObjectProperty(RDF_TYPE, HGQL_OBJECT_TYPE);
        typeNodes.addAll(rdfSchema.getSubjectsOfObjectProperty(RDF_TYPE, HGQL_QUERY_TYPE));

        typeNodes.forEach(rdfNode -> {
            val typeName = rdfSchema.getValueOfDataProperty(rdfNode, HGQL_HAS_NAME);
            val typeHref = rdfSchema.getValueOfObjectProperty(rdfNode, HGQL_HREF);
            val typeURI = (typeHref != null) ? typeHref.asResource().getURI() : null;

            final List<RDFNode> fieldsOfType = rdfSchema.getValuesOfObjectProperty(rdfNode, HGQL_HAS_FIELD);
            final Map<String, FieldOfTypeConfig> fieldConfig = new HashMap<>();

            fieldsOfType.forEach(field -> {

                val fieldOfTypeName = rdfSchema.getValueOfDataProperty(field, HGQL_HAS_NAME);
                val href = rdfSchema.getValueOfObjectProperty(field, HGQL_HREF);
                val hrefURI = (href != null) ? href.asResource().getURI() : null;
                val serviceNode = rdfSchema.getValueOfObjectProperty(field, HGQL_HAS_SERVICE);
                val serviceId = rdfSchema.getValueOfDataProperty(serviceNode, HGQL_HAS_ID);
                val outputTypeNode = rdfSchema.getValueOfObjectProperty(field, HGQL_OUTPUT_TYPE);
                val graphqlOutputType = getGraphQLOutputType(outputTypeNode);
                val isList = getIsList(outputTypeNode);
                val targetTypeName = getTargetTypeName(outputTypeNode);
                val fieldOfTypeConfig = new FieldOfTypeConfig(fieldOfTypeName, hrefURI, services.get(serviceId), graphqlOutputType, isList, targetTypeName);
                fieldConfig.put(fieldOfTypeName, fieldOfTypeConfig);
            });

            val typeConfig = new TypeConfig(typeName, typeURI, fieldConfig);
            this.types.put(typeName, typeConfig);
        });
    }

    private String getTargetTypeName(RDFNode outputTypeNode) {
        String typeName = rdfSchema.getValueOfDataProperty(outputTypeNode, HGQL_HAS_NAME);
        if (typeName != null) {
            return typeName;
        } else {
            RDFNode childOutputNode = rdfSchema.getValueOfObjectProperty(outputTypeNode, HGQL_OF_TYPE);
            return getTargetTypeName(childOutputNode);
        }
    }

    private Boolean getIsList(final RDFNode outputTypeNode) {
        val outputNode = rdfSchema.getValueOfObjectProperty(outputTypeNode, RDF_TYPE);
        val typeURI = outputNode.asResource().getURI();
        if (typeURI.equals(HGQL_LIST_TYPE)) {
            return true;
        } else {
            val childOutputNode = rdfSchema.getValueOfObjectProperty(outputTypeNode, HGQL_OF_TYPE);
            if (childOutputNode != null) {
                return getIsList(childOutputNode);
            } else {
                return false;
            }
        }
    }

    private GraphQLOutputType getGraphQLOutputType(final RDFNode outputTypeNode) {
        val outputNode = rdfSchema.getValueOfObjectProperty(outputTypeNode, RDF_TYPE);
        val typeURI = outputNode.asResource().getURI();
        if (typeURI.equals(HGQL_SCALAR_TYPE)) {
            return SCALAR_TYPES_TO_GRAPHQL_OUTPUT.get(outputTypeNode.asResource().getURI());
        }
        if (typeURI.equals(HGQL_OBJECT_TYPE)) {
            val typeName = rdfSchema.getValueOfDataProperty(outputTypeNode, HGQL_HAS_NAME);
            return new GraphQLTypeReference(typeName);
        }
        if (typeURI.equals(HGQL_LIST_TYPE)) {
            val childOutputNode = rdfSchema.getValueOfObjectProperty(outputTypeNode, HGQL_OF_TYPE);
            return new GraphQLList(getGraphQLOutputType(childOutputNode));
        }
        if (typeURI.equals(HGQL_NON_NULL_TYPE)) {
            val childOutputNode = rdfSchema.getValueOfObjectProperty(outputTypeNode, HGQL_OF_TYPE);
            return new GraphQLNonNull(getGraphQLOutputType(childOutputNode));
        }
        return null;
    }

    private String getOutputType(final Type type) {

        if (type.getClass() == TypeName.class) {
            val castType = (TypeName) type;
            val name = castType.getName();

            if (SCALAR_TYPES.containsKey(name)) {
                return SCALAR_TYPES.get(name);
            } else {
                return schemaNamespace + name;
            }
        }

        val dummyNode = schemaNamespace + UUID.randomUUID();

        if (type.getClass() == ListType.class) {
            val castType = (ListType) type;
            val subTypeUri = getOutputType(castType.getType());
            rdfSchema.insertObjectTriple(dummyNode, RDF_TYPE, HGQL_LIST_TYPE);
            rdfSchema.insertObjectTriple(dummyNode, HGQL_OF_TYPE, subTypeUri);
        }

        if (type.getClass() == NonNullType.class) {
            val castType = (NonNullType) type;
            val subTypeUri = getOutputType(castType.getType());
            rdfSchema.insertObjectTriple(dummyNode, RDF_TYPE, HGQL_NON_NULL_TYPE);
            rdfSchema.insertObjectTriple(dummyNode, HGQL_OF_TYPE, subTypeUri);
        }

        return dummyNode;
    }

    public String getSchemaNamespace() {
        return schemaNamespace;
    }

    private void populateHGQLRDFSchemaBaseTypes() {

        rdfSchema.insertObjectTriple(schemaUri, RDF_TYPE, HGQL_SCHEMA);
        rdfSchema.insertObjectTriple(schemaNamespace + "query", RDF_TYPE, HGQL_QUERY_TYPE);
        rdfSchema.insertStringLiteralTriple(schemaNamespace + "query", HGQL_HAS_NAME, "Query");
        rdfSchema.insertObjectTriple(HGQL_STRING, RDF_TYPE, HGQL_SCALAR_TYPE);
        rdfSchema.insertStringLiteralTriple(HGQL_STRING, HGQL_HAS_NAME, "String");
        rdfSchema.insertObjectTriple(HGQL_INT, RDF_TYPE, HGQL_SCALAR_TYPE);
        rdfSchema.insertStringLiteralTriple(HGQL_INT, HGQL_HAS_NAME, "Int");
        rdfSchema.insertObjectTriple(HGQL_BOOLEAN, RDF_TYPE, HGQL_SCALAR_TYPE);
        rdfSchema.insertStringLiteralTriple(HGQL_BOOLEAN, HGQL_HAS_NAME, "Boolean");
        rdfSchema.insertObjectTriple(HGQL_ID, RDF_TYPE, HGQL_SCALAR_TYPE);
        rdfSchema.insertStringLiteralTriple(HGQL_ID, HGQL_HAS_NAME, "ID");
    }

    private Map<String, String> generateContextMap(final TypeDefinition context) {
        final List<Node> children = context.getChildren();
        final Map<String, String> contextMap = new HashMap<>();

        children.forEach(node -> {
            val field = (FieldDefinition) node;
            val iri = ((StringValue) field.getDirective("href").getArgument("iri").getValue()).getValue();
            contextMap.put(field.getName(), iri);
        });
        return contextMap;
    }

    private void populateServices(final Map<String, Service> services) {
        final Collection<String> serviceIds = services.keySet();
        serviceIds.forEach(serviceId -> {
            val serviceURI = HGQL_SERVICE_NAMESPACE + serviceId;
            rdfSchema.insertObjectTriple(serviceURI, RDF_TYPE, HGQL_SERVICE);
            rdfSchema.insertStringLiteralTriple(serviceURI, HGQL_HAS_ID, serviceId);
        });
    }

    private void populateTypes(
            final Map<String, TypeDefinition> registryTypes,
            final Map<String, String> contextMap
    ) {
        final Collection<String> typeNames = registryTypes.keySet();
        typeNames.remove("__Context");

        typeNames.forEach(typeName -> {

            val typeUri = schemaNamespace + typeName;
            rdfSchema.insertStringLiteralTriple(typeUri, HGQL_HAS_NAME, typeName);
            rdfSchema.insertObjectTriple(typeUri, HGQL_HREF, contextMap.get(typeName));
            rdfSchema.insertObjectTriple(typeUri, RDF_TYPE, HGQL_OBJECT_TYPE);


            val type = registryTypes.get(typeName);
            populateDirectives(type, typeName, typeUri);
            populateChildTypes(type, typeName, typeUri, contextMap);
        });
    }

    private void populateDirectives(
            final TypeDefinition type,
            final String typeName,
            final String typeUri
    ) {
        final List<Directive> directives = type.getDirectives();
        directives.forEach(dir -> {
            if (dir.getName().equals("service")) {
                val getQueryUri = typeUri + "_GET";
                val getByIdQueryUri = typeUri + "_GET_BY_ID";

                rdfSchema.insertObjectTriple(getQueryUri, RDF_TYPE, HGQL_QUERY_FIELD);
                rdfSchema.insertObjectTriple(getQueryUri, RDF_TYPE, HGQL_QUERY_GET_FIELD);
                rdfSchema.insertObjectTriple(schemaNamespace + "query", HGQL_HAS_FIELD, getQueryUri);
                rdfSchema.insertStringLiteralTriple(getQueryUri, HGQL_HAS_NAME, typeName + "_GET");
                rdfSchema.insertObjectTriple(getByIdQueryUri, RDF_TYPE, HGQL_QUERY_FIELD);
                rdfSchema.insertObjectTriple(getByIdQueryUri, RDF_TYPE, HGQL_QUERY_GET_BY_ID_FIELD);
                rdfSchema.insertObjectTriple(schemaNamespace + "query", HGQL_HAS_FIELD, getByIdQueryUri);
                rdfSchema.insertStringLiteralTriple(getByIdQueryUri, HGQL_HAS_NAME, typeName + "_GET_BY_ID");

                val outputListTypeURI = schemaNamespace + UUID.randomUUID();

                rdfSchema.insertObjectTriple(outputListTypeURI, RDF_TYPE, HGQL_LIST_TYPE);
                rdfSchema.insertObjectTriple(outputListTypeURI, HGQL_OF_TYPE, typeUri);

                rdfSchema.insertObjectTriple(getQueryUri, HGQL_OUTPUT_TYPE, outputListTypeURI);
                rdfSchema.insertObjectTriple(getByIdQueryUri, HGQL_OUTPUT_TYPE, outputListTypeURI);
                val serviceId = ((StringValue) dir.getArgument("id").getValue()).getValue();

                val serviceURI = HGQL_SERVICE_NAMESPACE + serviceId;
                rdfSchema.insertObjectTriple(getQueryUri, HGQL_HAS_SERVICE, serviceURI);
                rdfSchema.insertObjectTriple(getByIdQueryUri, HGQL_HAS_SERVICE, serviceURI);
            }
        });
    }

    private void populateChildTypes(
            final TypeDefinition type,
            final String typeName,
            final String typeUri,
            final Map<String, String> contextMap
    ) {

        type.getChildren().forEach(node -> {
            if (node.getClass().isAssignableFrom(FieldDefinition.class)) {
                val field = (FieldDefinition) node;
                val fieldURI = schemaNamespace + typeName + "/" + field.getName();

                rdfSchema.insertStringLiteralTriple(fieldURI, HGQL_HAS_NAME, field.getName());
                rdfSchema.insertObjectTriple(fieldURI, HGQL_HREF, contextMap.get(field.getName()));

                rdfSchema.insertObjectTriple(fieldURI, RDF_TYPE, HGQL_FIELD);
                rdfSchema.insertObjectTriple(typeUri, HGQL_HAS_FIELD, fieldURI);

                val serviceId = ((StringValue) field.getDirective("service").getArgument("id").getValue()).getValue();
                val serviceURI = HGQL_SERVICE_NAMESPACE + serviceId;
                rdfSchema.insertObjectTriple(fieldURI, HGQL_HAS_SERVICE, serviceURI);
                rdfSchema.insertObjectTriple(serviceURI, RDF_TYPE, HGQL_SERVICE);

                val outputTypeUri = getOutputType(field.getType());
                rdfSchema.insertObjectTriple(fieldURI, HGQL_OUTPUT_TYPE, outputTypeUri);

            }
        });
    }
}
