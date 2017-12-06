package org.hypergraphql.datamodel;

import graphql.language.*;
import graphql.schema.GraphQLOutputType;
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


    private Map<String, TypeConfig> types;
    private Map<String, FieldConfig> fields;
    private Map<String, QueryFieldConfig> queryFields;

    private Model model = ModelFactory.createDefaultModel();

    public HGQLSchema(TypeDefinitionRegistry registry, String schemaName, Map<String, Service> services) throws Exception {

        THIS_SCHEMA_URI = HGQL_SCHEMA_NAMESPACE + schemaName;
        THIS_SCHEMA_NAMESPACE = THIS_SCHEMA_URI + "/";

        insertObjectTriple(THIS_SCHEMA_URI, RDF_TYPE, HGQL_SCHEMA);
        insertObjectTriple(THIS_SCHEMA_NAMESPACE + "query", RDF_TYPE, HGQL_QUERY_TYPE);
        insertStringLiteralTriple(THIS_SCHEMA_NAMESPACE + "query", HGQL_HAS_NAME, "Query");
        insertObjectTriple(HGQL_STRING, RDF_TYPE, HGQL_SCALAR_TYPE);
        insertObjectTriple(HGQL_Int, RDF_TYPE, HGQL_SCALAR_TYPE);
        insertObjectTriple(HGQL_Boolean, RDF_TYPE, HGQL_SCALAR_TYPE);


        Map<String, TypeDefinition> types = registry.types();

        TypeDefinition vocabulary = types.get("__Vocabulary");

        if (vocabulary==null) {
            Exception e = new Exception("The provided GraphQL schema IDL specification is missing the obligatory __Vocabulary type (see specs at http://hypergraphql.org).");
            logger.error(e);
            throw(e);
        }

        List<Node> children = vocabulary.getChildren();

        for (Node node : children) {
            FieldDefinition field = ((FieldDefinition) node);
            String iri = ((StringValue) field.getDirective("href").getArgument("iri").getValue()).getValue();
            insertObjectTriple(THIS_SCHEMA_NAMESPACE + field.getName(), HGQL_HREF, iri);
        }

        Set<String> typeNames = types.keySet();
        typeNames.remove("__Vocabulary");

        for (String typeName : typeNames) {

            String typeUri = THIS_SCHEMA_NAMESPACE + typeName;
            insertStringLiteralTriple(typeUri, HGQL_HAS_NAME, typeName);


            String getQueryUri = typeUri + "_GET";
            String getByIdQueryUri = typeUri + "_GET_BY_ID";

            insertObjectTriple(typeUri, RDF_TYPE, HGQL_OBJECT_TYPE);

            insertObjectTriple(getQueryUri, RDF_TYPE, HGQL_QUERY_FIELD);
            insertObjectTriple(getQueryUri, RDF_TYPE, HGQL_QUERY_GET_FIELD);
            insertObjectTriple(THIS_SCHEMA_NAMESPACE + "query", HGQL_HAS_FIELD, getQueryUri);
            insertStringLiteralTriple(getQueryUri, HGQL_HAS_NAME, typeName + "_GET");
            insertObjectTriple(getByIdQueryUri, RDF_TYPE, HGQL_QUERY_FIELD);
            insertObjectTriple(getByIdQueryUri, RDF_TYPE, HGQL_QUERY_GET_BY_ID_FIELD);
            insertObjectTriple(THIS_SCHEMA_NAMESPACE + "query", HGQL_HAS_FIELD, getByIdQueryUri);
            insertStringLiteralTriple(getByIdQueryUri, HGQL_HAS_NAME, typeName + "_GET_BY_ID");

            String outputListTypeURI = THIS_SCHEMA_NAMESPACE + UUID.randomUUID();

            insertObjectTriple(outputListTypeURI, RDF_TYPE, HGQL_LIST_TYPE);
            insertObjectTriple(outputListTypeURI, HGQL_OF_TYPE, typeUri);

            insertObjectTriple(getQueryUri, HGQL_OUTPUT_TYPE, outputListTypeURI);
            insertObjectTriple(getByIdQueryUri, HGQL_OUTPUT_TYPE, outputListTypeURI);

            TypeDefinition type = types.get(typeName);

            for (Directive dir : type.getDirectives()) {
                if (dir.getName().equals("service")) {
                    String serviceId = ((StringValue) dir.getArgument("id").getValue()).getValue();

                    String serviceURI = HGQL_SERVICE_NAMESPACE + serviceId;
                    insertObjectTriple(getQueryUri, HGQL_HAS_SERVICE, serviceURI);
                    insertObjectTriple(getByIdQueryUri, HGQL_HAS_SERVICE, serviceURI);
                    insertObjectTriple(serviceURI, RDF_TYPE, HGQL_SERVICE);
                    insertStringLiteralTriple(serviceURI, HGQL_HAS_ID, serviceId);
                }
            }

            List<Node> typeChildren = type.getChildren();

            for (Node node : typeChildren) {
                try {
                    FieldDefinition field = (FieldDefinition) node;
                    String fieldURI = THIS_SCHEMA_NAMESPACE + field.getName();

                    insertStringLiteralTriple(fieldURI, HGQL_HAS_NAME, field.getName());

                    insertObjectTriple(fieldURI, RDF_TYPE, HGQL_FIELD);
                    insertObjectTriple(typeUri, HGQL_HAS_FIELD, fieldURI);

                    String serviceId = ((StringValue) field.getDirective("service").getArgument("id").getValue()).getValue();
                    String serviceURI = HGQL_SERVICE_NAMESPACE + serviceId;
                    insertObjectTriple(fieldURI, HGQL_HAS_SERVICE, serviceURI);
                    insertObjectTriple(serviceURI, RDF_TYPE, HGQL_SERVICE);

                    String outputTypeUri = getOutputType(field.getType());
                    insertObjectTriple(fieldURI, HGQL_OUTPUT_TYPE, outputTypeUri);

                } catch(Exception e) {}



            }
        }

        model.write(System.out);

        generateConfigs(services);

    }

    private void generateConfigs(Map<String, Service> services) {
        this.types = new HashMap<>();
        this.fields = new HashMap<>();
        this.queryFields = new HashMap<>();

        List<RDFNode> fieldNodes = getSubjects(RDF_TYPE, HGQL_FIELD);

        for (RDFNode fieldNode : fieldNodes) {

            String name = getLiteralValueObject(fieldNode, HGQL_HAS_NAME);
            RDFNode href = getObject(fieldNode, HGQL_HREF);
            RDFNode serviceNode = getObject(fieldNode, HGQL_HAS_SERVICE);
            String serviceId = getLiteralValueObject(serviceNode, HGQL_HAS_ID);

            FieldConfig fieldConfig = new FieldConfig(href.asResource().getURI(), services.get(serviceId));
            fields.put(name, fieldConfig);
        }

        List<RDFNode> queryFieldNodes = getSubjects(RDF_TYPE, HGQL_QUERY_FIELD);
        List<RDFNode> queryGetFieldNodes = getSubjects(RDF_TYPE, HGQL_QUERY_GET_FIELD);

        for (RDFNode queryFieldNode : queryFieldNodes) {

            String name = getLiteralValueObject(queryFieldNode, HGQL_HAS_NAME);
            RDFNode serviceNode = getObject(queryFieldNode, HGQL_HAS_SERVICE);
            String serviceId = getLiteralValueObject(serviceNode, HGQL_HAS_ID);

            String type = (queryGetFieldNodes.contains(queryFieldNode)) ? HGQL_QUERY_GET_FIELD : HGQL_QUERY_GET_BY_ID_FIELD;
            QueryFieldConfig fieldConfig = new QueryFieldConfig(services.get(serviceId), type);
            queryFields.put(name, fieldConfig);
        }

        List<RDFNode> typeNodes = getSubjects(RDF_TYPE, HGQL_OBJECT_TYPE);
        typeNodes.addAll(getSubjects(RDF_TYPE, HGQL_QUERY_TYPE));

        for (RDFNode typeNode : typeNodes) {
            String name = getLiteralValueObject(typeNode.asResource().getURI(), HGQL_HAS_NAME);

            List<RDFNode> fieldsOfType = getObjects(typeNode.asResource().getURI(), HGQL_OBJECT_TYPE);

            for (RDFNode fieldOfType : fieldsOfType) {

                String fieldOfTypeName = getLiteralValueObject(fieldOfType, HGQL_HAS_NAME);
                RDFNode href = getObject(fieldOfType, HGQL_HREF);
                RDFNode serviceNode = getObject(fieldOfType, HGQL_HAS_SERVICE);
                String serviceId = getLiteralValueObject(serviceNode, HGQL_HAS_ID);



     //           new FieldOfTypeConfig(href.asResource().getURI(), services.get(serviceId), GraphQLOutputType graphqlOutputType, String typeName)
            }

        }


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
            insertObjectTriple(dummyNode, RDF_TYPE, HGQL_LIST_TYPE);
            insertObjectTriple(dummyNode, HGQL_OF_TYPE, subTypeUri);
        }

        if (type.getClass()==NonNullType.class) {
            ListType castType = (ListType) type;
            String subTypeUri = getOutputType(castType.getType());
            insertObjectTriple(dummyNode, RDF_TYPE, HGQL_NON_NULL_TYPE);
            insertObjectTriple(dummyNode, HGQL_OF_TYPE, subTypeUri);
        }

        return dummyNode;
    }

    private void insertObjectTriple(String subjectURI, String predicateURI, String objectURI) {

        Resource subject = model.getResource(subjectURI);
        Property predicate = model.getProperty(predicateURI);
        Resource object = model.getResource(objectURI);

        model.add(subject, predicate, object);

    }

    private void insertStringLiteralTriple(String subjectURI, String predicateURI, String value) {

        Resource subject = model.getResource(subjectURI);
        Property predicate = model.getProperty(predicateURI);

        model.add(subject, predicate, value);

    }

    private List<RDFNode> getObjects(RDFNode subject, String predicateURI) {
        Property predicate = model.getProperty(predicateURI);

        NodeIterator iterator = this.model.listObjectsOfProperty(subject.asResource(), predicate);
        List<RDFNode> nodeList = new ArrayList<>();

        while (iterator.hasNext()) {

            nodeList.add(iterator.next());
        }
        return nodeList;
    }

    private List<RDFNode> getObjects(String subjectURI, String predicateURI) {
        Resource subject = model.getResource(subjectURI);
        Property predicate = model.getProperty(predicateURI);

        NodeIterator iterator = this.model.listObjectsOfProperty(subject, predicate);
        List<RDFNode> nodeList = new ArrayList<>();

        while (iterator.hasNext()) {

            nodeList.add(iterator.next());
        }
        return nodeList;
    }

    private RDFNode getObject(String subjectURI, String predicateURI) {
        Resource subject = model.getResource(subjectURI);
        Property predicate = model.getProperty(predicateURI);

        NodeIterator iterator = this.model.listObjectsOfProperty(subject, predicate);
        return iterator.next();
    }

    private RDFNode getObject(RDFNode subject, String predicateURI) {
        Property predicate = model.getProperty(predicateURI);

        NodeIterator iterator = this.model.listObjectsOfProperty(subject.asResource(), predicate);
        return iterator.next();
    }

    private List<RDFNode> getSubjects(String predicateURI, String objectURI) {

        Property predicate = model.getProperty(predicateURI);
        Resource object = model.getResource(objectURI);

        ResIterator iterator = this.model.listSubjectsWithProperty(predicate, object);
        List<RDFNode> nodeList = new ArrayList<>();

        while (iterator.hasNext()) {

            nodeList.add(iterator.next());
        }
        return nodeList;
    }

    private String getLiteralValueObject(String subjectURI, String predicateURI) {
        Resource subject = model.getResource(subjectURI);
        Property predicate = model.getProperty(predicateURI);

        NodeIterator iterator = this.model.listObjectsOfProperty(subject, predicate);
        return iterator.next().asLiteral().getString();
    }

    private String getLiteralValueObject(RDFNode subject, String predicateURI) {
        Property predicate = model.getProperty(predicateURI);

        NodeIterator iterator = this.model.listObjectsOfProperty(subject.asResource(), predicate);
        return iterator.next().asLiteral().getString();
    }



}
