package org.hypergraphql.datamodel;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.log4j.Logger;
import org.hypergraphql.config.schema.HGQLVocabulary;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by szymon on 22/08/2017.
 */

// TODO - streams
public class ModelContainer {

    protected Model model;

    private static final Logger LOGGER = Logger.getLogger(ModelContainer.class);

    public String getDataOutput(String format) {

        StringWriter out = new StringWriter();
        model.write(out, format);
        return out.toString();
    }

    public ModelContainer(Model model) {
        this.model = model;
    }


    private Property getPropertyFromUri(String propertyURI) {

        return this.model.getProperty(propertyURI);
    }

    private Resource getResourceFromUri(String resourceURI) {

        return this.model.getResource(resourceURI);
    }

    List<RDFNode> getSubjectsOfObjectProperty(String predicateURI, String objectURI) {

        ResIterator iterator = this.model.listSubjectsWithProperty(getPropertyFromUri(predicateURI), getResourceFromUri(objectURI));
        List<RDFNode> nodeList = new ArrayList<>();
        iterator.forEachRemaining(nodeList::add);
        return nodeList;
    }

    String getValueOfDataProperty(RDFNode subject, String predicateURI) {

        return getValueOfDataProperty(subject, predicateURI, new HashMap<>());
    }

    String getValueOfDataProperty(RDFNode subject, String predicateURI, Map<String, Object> args) {

        final List<String> values = getValuesOfDataProperty(subject, predicateURI, args);
        if(values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    List<String> getValuesOfDataProperty(RDFNode subject, String predicateURI, Map<String, Object> args) {

        List<String> valList = new ArrayList<>();

        NodeIterator iterator = this.model.listObjectsOfProperty(subject.asResource(), getPropertyFromUri(predicateURI));

        while (iterator.hasNext()) {

            RDFNode data = iterator.next();

            if (data.isLiteral()) {
                if (args.containsKey("lang")) {
                    if (data.asLiteral().getLanguage().equals(args.get("lang").toString())) {
                        valList.add(data.asLiteral().getString());
                    }
                } else {
                    valList.add(data.asLiteral().getString());
                }
            }
        }
        return valList;
    }

    List<RDFNode> getValuesOfObjectProperty(String subjectURI, String predicateURI) {

        return getValuesOfObjectProperty(getResourceFromUri(subjectURI), predicateURI);
    }

    List<RDFNode> getValuesOfObjectProperty(RDFNode subject, String predicateURI) {

        return getValuesOfObjectProperty(subject, predicateURI, null);
    }

    List<RDFNode> getValuesOfObjectProperty(RDFNode subject, String predicateURI, String targetURI) {

        NodeIterator iterator = this.model.listObjectsOfProperty(subject.asResource(), getPropertyFromUri(predicateURI));
        List<RDFNode> rdfNodes = new ArrayList<>();
        iterator.forEachRemaining(node -> {
            if (!node.isLiteral()) {
                if(targetURI == null) {
                    rdfNodes.add(node);
                } else if(this.model.contains(node.asResource(), getPropertyFromUri(HGQLVocabulary.RDF_TYPE), getResourceFromUri(targetURI))) {
                    rdfNodes.add(node);
                }
            }
        });
        return rdfNodes;
    }

    RDFNode getValueOfObjectProperty(RDFNode subject, String predicateURI) {

        final List<RDFNode> values = getValuesOfObjectProperty(subject, predicateURI);
        if(values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    RDFNode getValueOfObjectProperty(RDFNode subject, String predicateURI, String targetURI) {

        NodeIterator iterator = this.model.listObjectsOfProperty(subject.asResource(), getPropertyFromUri(predicateURI));
        while (iterator.hasNext()) {

            RDFNode next = iterator.next();
            if (!next.isLiteral()) {
                if (targetURI != null && this.model.contains(next.asResource(), getPropertyFromUri(HGQLVocabulary.RDF_TYPE), getResourceFromUri(targetURI))) {
                    return next;
                }
            }
        }
        return null;
    }

    void insertObjectTriple(String subjectURI, String predicateURI, String objectURI) {

        model.add(getResourceFromUri(subjectURI), getPropertyFromUri(predicateURI), getResourceFromUri(objectURI));

    }

    void insertStringLiteralTriple(String subjectURI, String predicateURI, String value) {

        model.add(getResourceFromUri(subjectURI), getPropertyFromUri(predicateURI), value);

    }
}
