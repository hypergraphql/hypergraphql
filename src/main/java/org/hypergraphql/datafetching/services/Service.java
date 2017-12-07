package org.hypergraphql.datafetching.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.hypergraphql.config.schema.FieldConfig;
import org.hypergraphql.config.schema.QueryFieldConfig;
import org.hypergraphql.config.schema.TypeConfig;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.datafetching.TreeExecutionResult;

import java.util.Iterator;
import java.util.Set;

public abstract class Service {

    protected String type;
    protected String id;


    protected Class SPARQLEndpointService ;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }



    public abstract TreeExecutionResult executeQuery(JsonNode query, Set<String> input,  Set<String> strings);

    public abstract void setParameters(JsonNode jsonnode);

    public Model getModelFromResults(JsonNode query, QuerySolution results) {

        Model model = ModelFactory.createDefaultModel();
        if (query.isNull()) return model;

        if (query.isArray()) {


            Iterator<JsonNode> nodesIterator = query.elements();

            while (nodesIterator.hasNext()) {


                JsonNode currentNode = nodesIterator.next();

                Model currentmodel = buildmodel(results, currentNode);
                model.add(currentmodel);
                model.add(getModelFromResults(currentNode.get("fields"), results));

            }
        }

        else {

            Model currentModel = buildmodel(results,query);
            model.add(currentModel);
            model.add(getModelFromResults(query.get("fields"), results));

        }

        return model;

    }

    private Model buildmodel(QuerySolution results, JsonNode currentNode) {


        HGQLConfig config = HGQLConfig.getInstance();

        Model model = ModelFactory.createDefaultModel();

        FieldConfig propertyString = config.fields().get(currentNode.get("name").asText());
        TypeConfig targetTypeString = config.types().get(currentNode.get("targetName").asText());

        if (propertyString != null && !(currentNode.get("parentId").asText().equals("null"))) {
            Property predicate = model.createProperty("", propertyString.id());
            Resource subject = results.getResource(currentNode.get("parentId").asText());
            RDFNode object = results.get(currentNode.get("nodeId").asText());
            if (predicate!=null&&subject!=null&&object!=null)
                model.add(subject, predicate, object);
        }

        if (targetTypeString != null) {
            Resource subject = results.getResource(currentNode.get("nodeId").asText());
            Resource object = model.createResource(targetTypeString.id());
            if (subject!=null&&object!=null)
                model.add(subject, RDF.type, object);
        }

        QueryFieldConfig queryField = config.queryFields().get(currentNode.get("name").asText());

        if (queryField!=null) {

            String typeName = (currentNode.get("alias").isNull()) ? currentNode.get("name").asText() : currentNode.get("alias").asText();
            Resource object = results.getResource(currentNode.get("nodeId").asText());
            Resource subject = model.createResource(config.HGQL_QUERY_URI);
            Property predicate = model.createProperty("", config.HGQL_QUERY_PREFIX + typeName);
            model.add(subject, predicate, object);
        }
        return model;
    }



}