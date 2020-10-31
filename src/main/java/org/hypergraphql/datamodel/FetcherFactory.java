package org.hypergraphql.datamodel;

import graphql.language.Field;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLTypeUtil;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.jena.rdf.model.RDFNode;
import org.hypergraphql.config.schema.HGQLVocabulary;
import org.hypergraphql.config.schema.TypeConfig;
import org.hypergraphql.config.system.FetchParams;

@RequiredArgsConstructor
public class FetcherFactory {

    private final HGQLSchema schema;

    public DataFetcher<String> idFetcher() {

        return environment -> {
            final RDFNode thisNode = environment.getSource();

            if (thisNode.asResource().isURIResource()) {
                return thisNode.asResource().getURI();
            } else {
                return "_:" + thisNode.asNode().getBlankNodeLabel();
            }
        };
    }

    public DataFetcher<String> typeFetcher(Map<String, TypeConfig> types) {
        return environment -> {
            final var typeName = GraphQLTypeUtil.simplePrint(environment.getParentType());
            return (types.containsKey(typeName)) ? types.get(typeName).getId() : null;
        };
    }

    public DataFetcher<List<RDFNode>> instancesOfTypeFetcher() {
        return environment -> {
            final var field = (Field) environment.getFields().toArray()[0];
            final var predicate = (field.getAlias() == null) ? field.getName() : field.getAlias();
            final ModelContainer client = environment.getContext();
            return client.getValuesOfObjectProperty(
                    HGQLVocabulary.HGQL_QUERY_URI,
                    HGQLVocabulary.HGQL_QUERY_NAMESPACE + predicate
            );
        };
    }

    public DataFetcher<List<RDFNode>> objectsFetcher() {
        return environment -> {
            final var params = new FetchParams(environment, schema);
            return params.getClient().getValuesOfObjectProperty(
                    params.getSubjectResource(),
                    params.getPredicateURI(),
                    params.getTargetURI()
            );
        };
    }

    public DataFetcher<List<String>> literalValuesFetcher() {
        return environment -> {
            final var params = new FetchParams(environment, schema);
            return params.getClient().getValuesOfDataProperty(
                    params.getSubjectResource(),
                    params.getPredicateURI(),
                    environment.getArguments()
            );
        };
    }
}
