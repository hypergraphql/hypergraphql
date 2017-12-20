package org.hypergraphql.datafetching.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.hypergraphql.config.system.ServiceConfig;
import org.hypergraphql.datafetching.TreeExecutionResult;
import org.hypergraphql.datamodel.HGQLSchema;
import org.hypergraphql.datamodel.QueryNode;

import java.util.*;

public class LocalModelService extends Service {

    String filepath;
    String filetype;
    Model localmodel;

    @Override

    public TreeExecutionResult executeQuery(JsonNode query, Set<String> input, Set<String> markers , String rootType ,HGQLSchema schema) {

        Model model;
        Map<String, Set<String>> resultSet;

        model = getModelFromLocal(query , input ,schema);

        resultSet = getResultset(model, query, input, markers , schema);

        TreeExecutionResult treeExecutionResult = new TreeExecutionResult();
        treeExecutionResult.setResultSet(resultSet);
        treeExecutionResult.setModel(model);

        return treeExecutionResult;
    }

    private Model getModelFromLocal(JsonNode query, Set<String> input, HGQLSchema schema) {


       Model returnModel = ModelFactory.createDefaultModel();


        JsonNode node;

        if (!query.isArray())
            node = query.get("fields");
        else
            node = query;

        Set<LinkedList<QueryNode>> paths = getQueryPaths(node, schema);

        for (LinkedList<QueryNode> path : paths ) {

                returnModel.add(buildModelFromPath(localmodel,input,path));

        }



        return returnModel;


    }

    private Model buildModelFromPath(Model localmodel, Set<String> input, LinkedList<QueryNode> path) {

        Model returnmodel = ModelFactory.createDefaultModel();
        Set<String> objects;
        Set<String> subjects;
        if (input==null)
            objects = new HashSet<>();
        else objects=input;

        Iterator<QueryNode> iterator = path.iterator();

        while (iterator.hasNext()) {
            QueryNode queryNode = iterator.next();
            subjects = new HashSet<>(objects);
            objects = new HashSet<>();
            if (!subjects.isEmpty()){
                Iterator<String> subjectIterator = subjects.iterator();
                while (subjectIterator.hasNext()) {
                    String subject = subjectIterator.next();
                    Resource subjectresoource = localmodel.getResource(subject);
                    NodeIterator partialobjects = localmodel.listObjectsOfProperty(subjectresoource,queryNode.getNode());
                    while(partialobjects.hasNext()) {
                        RDFNode currentNode = partialobjects.next();
                        objects.add(currentNode.toString());
                        returnmodel.add(new StatementImpl(subjectresoource, queryNode.getNode(), currentNode));
                    }
                }

            }

            else {

                ResIterator subjectsIterator = localmodel.listResourcesWithProperty(queryNode.getNode());
                while (subjectsIterator.hasNext()) {
                    Resource subjectresoource = subjectsIterator.next();
                    NodeIterator partialobjects = localmodel.listObjectsOfProperty(subjectresoource, queryNode.getNode());
                    while (partialobjects.hasNext()) {
                        RDFNode currentNode = partialobjects.next();
                        objects.add(currentNode.toString());
                        returnmodel.add(new StatementImpl(subjectresoource, queryNode.getNode(), currentNode));
                    }
                }
            }




            }




        return returnmodel;

    }


    @Override
    public void setParameters(ServiceConfig serviceConfig) {

        this.id = serviceConfig.getId();
        this.filetype = serviceConfig.getFiletype();
        this.filepath = serviceConfig.getFilepath();
        this.localmodel = ModelFactory.createDefaultModel();
        this.localmodel.read(filepath,filetype);


    }
}
