package org.hypergraphql.datamodel;

import graphql.language.Field;
import graphql.schema.DataFetcher;
import org.apache.jena.rdf.model.RDFNode;
import org.hypergraphql.config.schema.HGQLVocabulary;
import org.hypergraphql.config.schema.TypeConfig;
import org.hypergraphql.config.system.FetchParams;

import java.util.List;
import java.util.Map;

public class FetcherFactory {

    private final HGQLSchema schema;

    public FetcherFactory(HGQLSchema hgqlSchema ) {

        this.schema = hgqlSchema;
    }

    public DataFetcher<String> idFetcher() {
        
        return environment -> {
            RDFNode thisNode = environment.getSource();

            if (thisNode.asResource().isURIResource()) {
                return thisNode.asResource().getURI();
            } else {
                return "_:" + thisNode.asNode().getBlankNodeLabel();
            }
        };
    }

    public DataFetcher<String> typeFetcher(Map<String, TypeConfig> types) {
        return  environment -> {
            String typeName = environment.getParentType().getName();
            return (types.containsKey(typeName)) ? types.get(typeName).getId() : null;
        };
    }

    public DataFetcher<List<RDFNode>> instancesOfTypeFetcher() {
        return environment -> {
            Field field = (Field) environment.getFields().toArray()[0];
            String predicate = (field.getAlias() == null) ? field.getName() : field.getAlias();
            ModelContainer client = environment.getContext();
            return client.getValuesOfObjectProperty(
                    HGQLVocabulary.HGQL_QUERY_URI,
                    HGQLVocabulary.HGQL_QUERY_NAMESPACE + predicate
            );
        };
    }

    public DataFetcher<RDFNode> objectFetcher() {
        return environment -> {
            FetchParams params = new FetchParams(environment, schema);
            return params.getClient().getValueOfObjectProperty(
                    params.getSubjectResource(),
                    params.getPredicateURI(),
                    params.getTargetURI()
            );
        };
    }

    public DataFetcher<List<RDFNode>> objectsFetcher() {
        return environment -> {
            FetchParams params = new FetchParams(environment, schema);
            return params.getClient().getValuesOfObjectProperty(
                    params.getSubjectResource(),
                    params.getPredicateURI(),
                    params.getTargetURI()
            );
        };
    }

    public DataFetcher<String> literalValueFetcher() {
        return environment -> {
            FetchParams params = new FetchParams(environment, schema);
            return params.getClient().getValueOfDataProperty(
                    params.getSubjectResource(),
                    params.getPredicateURI(),
                    environment.getArguments()
            );
        };
    }

    public DataFetcher<List<String>> literalValuesFetcher() {
        return environment -> {
            FetchParams params = new FetchParams(environment, schema);
            return params.getClient().getValuesOfDataProperty(
                    params.getSubjectResource(),
                    params.getPredicateURI(),
                    environment.getArguments()
            );
        };
    }
}
