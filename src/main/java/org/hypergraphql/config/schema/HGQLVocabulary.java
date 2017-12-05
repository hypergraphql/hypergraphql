package org.hypergraphql.config.schema;

import java.util.HashMap;
import java.util.Map;

public class HGQLVocabulary {

    public static final String HGQL_NAMESPACE = "http://hypergraphql.org/";
    public static final String HGQL_QUERY_URI = HGQL_NAMESPACE + "query";
    public static final String HGQL_QUERY_PREFIX = HGQL_QUERY_URI + "/";

    public static final Map<String, String> JSONLD_VOC = new HashMap<String, String>() {{
        put("_id", "@id");
        put("_type", "@type");
    }};

}
