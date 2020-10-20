package org.hypergraphql.config.schema;

import graphql.schema.GraphQLOutputType;
import java.util.Map;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLID;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static org.hypergraphql.util.HGQLConstants.ID;
import static org.hypergraphql.util.HGQLConstants.TYPE;

public abstract class HGQLVocabulary {

    public static final String HGQL_PREFIX = "hgql:";
    public static final String HGQL_SCHEMA_PREFIX = "hgql-schema:";
    public static final String HGQL_NAMESPACE = "http://hypergraphql.org/";
    public static final String HGQL_SCHEMA_NAMESPACE = HGQL_NAMESPACE + "schema/";
    public static final String HGQL_QUERY_URI = HGQL_NAMESPACE + "query";
    public static final String HGQL_QUERY_NAMESPACE = HGQL_QUERY_URI + "/";
    public static final String HGQL_OBJECT_TYPE = HGQL_NAMESPACE + "ObjectType";
    public static final String HGQL_SCALAR_TYPE = HGQL_NAMESPACE + "ScalarType";
    public static final String HGQL_STRING = HGQL_NAMESPACE + "String";
    public static final String HGQL_INT = HGQL_NAMESPACE + "Int";
    public static final String HGQL_BOOLEAN = HGQL_NAMESPACE + "Boolean";
    public static final String HGQL_ID = HGQL_NAMESPACE + "ID";
    public static final String HGQL_LIST_TYPE = HGQL_NAMESPACE + "ListType";
    public static final String HGQL_NON_NULL_TYPE = HGQL_NAMESPACE + "NonNullType";
    public static final String HGQL_QUERY_TYPE = HGQL_NAMESPACE + "QueryType";
    public static final String HGQL_HREF = HGQL_NAMESPACE + "href";
    public static final String HGQL_SCHEMA = HGQL_NAMESPACE + "Schema";
    public static final String HGQL_FIELD = HGQL_NAMESPACE + "Field";
    public static final String HGQL_QUERY_FIELD = HGQL_NAMESPACE + "QueryField";
    public static final String HGQL_QUERY_GET_FIELD = HGQL_NAMESPACE + "QueryGetField";
    public static final String HGQL_QUERY_GET_BY_ID_FIELD = HGQL_NAMESPACE + "QueryGetByIdField";
    public static final String HGQL_HAS_FIELD = HGQL_NAMESPACE + "field";
    public static final String HGQL_SERVICE = HGQL_NAMESPACE + "Service";
    public static final String HGQL_HAS_SERVICE = HGQL_NAMESPACE + "service";
    public static final String HGQL_HAS_NAME = HGQL_NAMESPACE + "name";
    public static final String HGQL_HAS_ID = HGQL_NAMESPACE + "id";
    public static final String HGQL_SERVICE_NAMESPACE = HGQL_HAS_SERVICE + "/";
    public static final String HGQL_OUTPUT_TYPE = HGQL_NAMESPACE + "outputType";
    public static final String HGQL_OF_TYPE = HGQL_NAMESPACE + "ofType";
    public static final String HGQL_KIND = HGQL_NAMESPACE + "kind";

    public static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    public static final Map<String, String> SCALAR_TYPES = Map.of(
        "String", HGQL_STRING,
        "Int", HGQL_INT,
        "Boolean", HGQL_BOOLEAN,
        "ID", HGQL_ID
    );

    public static final Map<String, GraphQLOutputType> SCALAR_TYPES_TO_GRAPHQL_OUTPUT =
            Map.of(
                HGQL_STRING, GraphQLString,
                HGQL_INT, GraphQLInt,
                HGQL_BOOLEAN, GraphQLBoolean,
                HGQL_ID, GraphQLID
            );

    public static final Map<String, String> JSONLD = Map.of(
        ID, "@id",
        TYPE, "@type"
    );
}
