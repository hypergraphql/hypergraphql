package org.hypergraphql.datamodel;

import graphql.language.*;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.apache.jena.rdf.model.*;
import org.apache.log4j.Logger;
import org.hypergraphql.config.schema.FieldConfig;
import org.hypergraphql.config.schema.FieldOfTypeConfig;
import org.hypergraphql.config.schema.QueryFieldConfig;
import org.hypergraphql.config.schema.TypeConfig;
import org.hypergraphql.datafetching.services.Service;

import java.util.*;

import static org.hypergraphql.config.schema.HGQLVocabulary.*;


public class HGQLSchema {

    private static Logger logger = Logger.getLogger(HGQLSchema.class);

    private String THIS_SCHEMA_URI;
    private String THIS_SCHEMA_NAMESPACE;


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

    public HGQLSchema(TypeDefinitionRegistry registry, String schemaName, Map<String, Service> services) throws Exception {

        THIS_SCHEMA_URI = HGQL_SCHEMA_NAMESPACE + schemaName;
        THIS_SCHEMA_NAMESPACE = THIS_SCHEMA_URI + "/";

        rdfSchema.insertObjectTriple(THIS_SCHEMA_URI, RDF_TYPE, HGQL_SCHEMA);
        rdfSchema.insertObjectTriple(THIS_SCHEMA_NAMESPACE + "query", RDF_TYPE, HGQL_QUERY_TYPE);
        rdfSchema.insertStringLiteralTriple(THIS_SCHEMA_NAMESPACE + "query", HGQL_HAS_NAME, "Query");
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

        if (context==null) {
            Exception e = new Exception("The provided GraphQL schema IDL specification is missing the obligatory __Context type (see specs at http://hypergraphql.org).");
            logger.error(e);
            throw(e);
        }

        List<Node> children = context.getChildren();

        Map<String, String> contextMap = new HashMap<>();

        for (Node node : children) {
            FieldDefinition field = ((FieldDefinition) node);
            String iri = ((StringValue) field.getDirective("href").getArgument("iri").getValue()).getValue();
            contextMap.put(field.getName(), iri);
        }

        Set<String> typeNames = types.keySet();
        typeNames.remove("__Context");

        Set<String> serviceIds = services.keySet();

        for (String serviceId : serviceIds) {
            String serviceURI = HGQL_SERVICE_NAMESPACE + serviceId;
            rdfSchema.insertObjectTriple(serviceURI, RDF_TYPE, HGQL_SERVICE);
            rdfSchema.insertStringLiteralTriple(serviceURI, HGQL_HAS_ID, serviceId);
        }


        for (String typeName : typeNames) {

            String typeUri = THIS_SCHEMA_NAMESPACE + typeName;
            rdfSchema.insertStringLiteralTriple(typeUri, HGQL_HAS_NAME, typeName);
            rdfSchema.insertObjectTriple(typeUri, HGQL_HREF, contextMap.get(typeName));
            rdfSchema.insertObjectTriple(typeUri, RDF_TYPE, HGQL_OBJECT_TYPE);


            TypeDefinition type = types.get(typeName);

            for (Directive dir : type.getDirectives()) {
                if (dir.getName().equals("service")) {
                    String getQueryUri = typeUri + "_GET";
                    String getByIdQueryUri = typeUri + "_GET_BY_ID";

                    rdfSchema.insertObjectTriple(getQueryUri, RDF_TYPE, HGQL_QUERY_FIELD);
                    rdfSchema.insertObjectTriple(getQueryUri, RDF_TYPE, HGQL_QUERY_GET_FIELD);
                    rdfSchema.insertObjectTriple(THIS_SCHEMA_NAMESPACE + "query", HGQL_HAS_FIELD, getQueryUri);
                    rdfSchema.insertStringLiteralTriple(getQueryUri, HGQL_HAS_NAME, typeName + "_GET");
                    rdfSchema.insertObjectTriple(getByIdQueryUri, RDF_TYPE, HGQL_QUERY_FIELD);
                    rdfSchema.insertObjectTriple(getByIdQueryUri, RDF_TYPE, HGQL_QUERY_GET_BY_ID_FIELD);
                    rdfSchema.insertObjectTriple(THIS_SCHEMA_NAMESPACE + "query", HGQL_HAS_FIELD, getByIdQueryUri);
                    rdfSchema.insertStringLiteralTriple(getByIdQueryUri, HGQL_HAS_NAME, typeName + "_GET_BY_ID");

                    String outputListTypeURI = THIS_SCHEMA_NAMESPACE + UUID.randomUUID();

                    rdfSchema.insertObjectTriple(outputListTypeURI, RDF_TYPE, HGQL_LIST_TYPE);
                    rdfSchema.insertObjectTriple(outputListTypeURI, HGQL_OF_TYPE, typeUri);

                    rdfSchema.insertObjectTriple(getQueryUri, HGQL_OUTPUT_TYPE, outputListTypeURI);
                    rdfSchema.insertObjectTriple(getByIdQueryUri, HGQL_OUTPUT_TYPE, outputListTypeURI);
                    String serviceId = ((StringValue) dir.getArgument("id").getValue()).getValue();

                    String serviceURI = HGQL_SERVICE_NAMESPACE + serviceId;
                    rdfSchema.insertObjectTriple(getQueryUri, HGQL_HAS_SERVICE, serviceURI);
                    rdfSchema.insertObjectTriple(getByIdQueryUri, HGQL_HAS_SERVICE, serviceURI);
                }
            }

            List<Node> typeChildren = type.getChildren();

            for (Node node : typeChildren) {
                if (node.getClass().getSimpleName().equals("FieldDefinition")) {
                    FieldDefinition field = (FieldDefinition) node;
                    String fieldURI = THIS_SCHEMA_NAMESPACE + typeName + "/" + field.getName();

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
            }
        }

        generateConfigs(services);

    }

    private void generateConfigs(Map<String, Service> services) {
        this.types = new HashMap<>();
        this.fields = new HashMap<>();
        this.queryFields = new HashMap<>();

        List<RDFNode> fieldNodes = rdfSchema.getSubjectsOfObjectProperty(RDF_TYPE, HGQL_FIELD);

        for (RDFNode fieldNode : fieldNodes) {

            String name = rdfSchema.getValueOfDataProperty(fieldNode, HGQL_HAS_NAME);
            RDFNode href = rdfSchema.getValueOfObjectProperty(fieldNode, HGQL_HREF);
            RDFNode serviceNode = rdfSchema.getValueOfObjectProperty(fieldNode, HGQL_HAS_SERVICE);
            String serviceId = rdfSchema.getValueOfDataProperty(serviceNode, HGQL_HAS_ID);

            FieldConfig fieldConfig = new FieldConfig(href.asResource().getURI(), services.get(serviceId));
            fields.put(name, fieldConfig);
        }

        List<RDFNode> queryFieldNodes = rdfSchema.getSubjectsOfObjectProperty(RDF_TYPE, HGQL_QUERY_FIELD);
        List<RDFNode> queryGetFieldNodes = rdfSchema.getSubjectsOfObjectProperty(RDF_TYPE, HGQL_QUERY_GET_FIELD);

        for (RDFNode queryFieldNode : queryFieldNodes) {

            String name = rdfSchema.getValueOfDataProperty(queryFieldNode, HGQL_HAS_NAME);
            RDFNode serviceNode = rdfSchema.getValueOfObjectProperty(queryFieldNode, HGQL_HAS_SERVICE);
            String serviceId = rdfSchema.getValueOfDataProperty(serviceNode, HGQL_HAS_ID);

            String type = (queryGetFieldNodes.contains(queryFieldNode)) ? HGQL_QUERY_GET_FIELD : HGQL_QUERY_GET_BY_ID_FIELD;
            QueryFieldConfig fieldConfig = new QueryFieldConfig(services.get(serviceId), type);
            queryFields.put(name, fieldConfig);

        }

        List<RDFNode> typeNodes = rdfSchema.getSubjectsOfObjectProperty(RDF_TYPE, HGQL_OBJECT_TYPE);
        typeNodes.addAll(rdfSchema.getSubjectsOfObjectProperty(RDF_TYPE, HGQL_QUERY_TYPE));

        for (RDFNode typeNode : typeNodes) {
            String typeName = rdfSchema.getValueOfDataProperty(typeNode, HGQL_HAS_NAME);
            RDFNode typeHref = rdfSchema.getValueOfObjectProperty(typeNode, HGQL_HREF);
            String typeURI = (typeHref!=null) ? typeHref.asResource().getURI() : null;

            List<RDFNode> fieldsOfType = rdfSchema.getValuesOfObjectProperty(typeNode, HGQL_HAS_FIELD);
            Map<String, FieldOfTypeConfig> fields = new HashMap<>();

            for (RDFNode fieldOfType : fieldsOfType) {

                String fieldOfTypeName = rdfSchema.getValueOfDataProperty(fieldOfType, HGQL_HAS_NAME);
                RDFNode href = rdfSchema.getValueOfObjectProperty(fieldOfType, HGQL_HREF);
                String hrefURI = (href!=null) ? href.asResource().getURI() : null;
                RDFNode serviceNode = rdfSchema.getValueOfObjectProperty(fieldOfType, HGQL_HAS_SERVICE);
                String serviceId = rdfSchema.getValueOfDataProperty(serviceNode, HGQL_HAS_ID);
                RDFNode outputTypeNode = rdfSchema.getValueOfObjectProperty(fieldOfType, HGQL_OUTPUT_TYPE);
                GraphQLOutputType graphqlOutputType = getGraphQLOutputType(outputTypeNode);
                Boolean isList = getIsList(outputTypeNode);
                String targetTypeName = getTargetTypeName(outputTypeNode);

                FieldOfTypeConfig fieldOfTypeConfig = new FieldOfTypeConfig(fieldOfTypeName, hrefURI, services.get(serviceId), graphqlOutputType, isList, targetTypeName);
                fields.put(fieldOfTypeName, fieldOfTypeConfig);

            }



            TypeConfig typeConfig = new TypeConfig(typeName, typeURI, fields);

            this.types.put(typeName, typeConfig);

        }


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

        if (type.getClass()==TypeName.class) {
            TypeName castType = (TypeName) type;
            String name = castType.getName();

            if (SCALAR_TYPES.containsKey(name)) {
                return SCALAR_TYPES.get(name);
            } else return THIS_SCHEMA_NAMESPACE + name;
        }

        String dummyNode = THIS_SCHEMA_NAMESPACE + UUID.randomUUID();

        if (type.getClass()==ListType.class) {
            ListType castType = (ListType) type;
            String subTypeUri = getOutputType(castType.getType());
            rdfSchema.insertObjectTriple(dummyNode, RDF_TYPE, HGQL_LIST_TYPE);
            rdfSchema.insertObjectTriple(dummyNode, HGQL_OF_TYPE, subTypeUri);
        }

        if (type.getClass()==NonNullType.class) {
            ListType castType = (ListType) type;
            String subTypeUri = getOutputType(castType.getType());
            rdfSchema.insertObjectTriple(dummyNode, RDF_TYPE, HGQL_NON_NULL_TYPE);
            rdfSchema.insertObjectTriple(dummyNode, HGQL_OF_TYPE, subTypeUri);
        }

        return dummyNode;
    }

}
