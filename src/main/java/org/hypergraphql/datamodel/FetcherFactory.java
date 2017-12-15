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


    private final HGQLSchema hgqlschema;

    public FetcherFactory(HGQLSchema hgqlSchema ) {

        this.hgqlschema = hgqlSchema;
    }





    public   DataFetcher<String> idFetcher() {
        
        return  environment -> {
            RDFNode thisNode = environment.getSource();

            if (thisNode.asResource().isURIResource()) {

                return thisNode.asResource().getURI();

            } else {
                return "_:" + thisNode.asNode().getBlankNodeLabel();
            }
        };
    }


    public   DataFetcher<List<RDFNode>> objectsFetcher() {
        return environment -> {

            FetchParams params = new FetchParams(environment,hgqlschema);
            return params.getClient().getValuesOfObjectProperty(params.getSubjectResource(), params.getPredicateURI(), params.getTargetURI());
        };
    }

    public  DataFetcher<String> typeFetcher(Map<String, TypeConfig> types) { return  environment -> {
        String typeName = environment.getParentType().getName();
        String type = (types.containsKey(typeName)) ? types.get(typeName).getId() : null;
        return type;
    };
    }

    public  DataFetcher<String> literalValueFetcher() { return
        environment -> {

            FetchParams params = new FetchParams(environment, hgqlschema);
            return params.getClient().getValueOfDataProperty(params.getSubjectResource(), params.getPredicateURI(), environment.getArguments());

        };
    }


    public  DataFetcher<List<RDFNode>> instancesOfTypeFetcher() {
        return environment -> {
            Field field = (Field) environment.getFields().toArray()[0];
            String predicate = (field.getAlias() != null) ? field.getAlias() : field.getName();
            ModelContainer client = environment.getContext();
            List<RDFNode> subjects = client.getValuesOfObjectProperty(HGQLVocabulary.HGQL_QUERY_URI, HGQLVocabulary.HGQL_QUERY_NAMESPACE + predicate);
            return subjects;
        };
    }


    public  DataFetcher<RDFNode> objectFetcher() {
        return environment -> {

            FetchParams params = new FetchParams(environment, hgqlschema);
            return params.getClient().getValueOfObjectProperty(params.getSubjectResource(), params.getPredicateURI(), params.getTargetURI());
        };
    }

    public   DataFetcher<List<String>> literalValuesFetcher() {
        return environment -> {

            FetchParams params = new FetchParams(environment, hgqlschema);
            return params.getClient().getValuesOfDataProperty(params.getSubjectResource(), params.getPredicateURI(), environment.getArguments());
        };
    }





}
