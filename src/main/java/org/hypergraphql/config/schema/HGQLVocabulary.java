package org.hypergraphql.config.schema;

import java.util.HashMap;
import java.util.Map;

public class HGQLVocabulary {

    public static final String HGQL_PREFIX= "hgql:";
    public static final String HGQL_SCHEMA_PREFIX = "hgql-schema:";
    public static final String HGQL_NAMESPACE = "http://hypergraphql.org/";
    public static final String HGQL_QUERY_URI = HGQL_NAMESPACE + "query";
    public static final String HGQL_QUERY_NAMESPACE = HGQL_QUERY_URI + "/";

    public static final Map<String, String> JSONLD = new HashMap<String, String>() {{
        put("_id", "@id");
        put("_type", "@type");
    }};

}
