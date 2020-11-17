package org.hypergraphql.datamodel;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.hypergraphql.config.schema.HGQLVocabulary;

/**
 * Created by szymon on 22/08/2017.
 */
@RequiredArgsConstructor
public class ModelContainer {

    private final Model model;

    public String getDataOutput(final String format) {

        val out = new StringWriter();
        model.write(out, format);
        return out.toString();
    }

    private Property getPropertyFromUri(final String propertyURI) {

        return this.model.getProperty(propertyURI);
    }

    private Resource getResourceFromUri(final String resourceURI) {

        return this.model.getResource(resourceURI);
    }

    List<RDFNode> getSubjectsOfObjectProperty(final String predicateURI, final String objectURI) {

        val iterator = this.model.listSubjectsWithProperty(getPropertyFromUri(predicateURI), getResourceFromUri(objectURI));
        final List<RDFNode> nodeList = new ArrayList<>();
        iterator.forEachRemaining(nodeList::add);
        return nodeList;
    }

    String getValueOfDataProperty(final RDFNode subject, final String predicateURI) {

        return getValueOfDataProperty(subject, predicateURI, new HashMap<>());
    }

    String getValueOfDataProperty(final RDFNode subject,
                                  final String predicateURI,
                                  final Map<String, Object> args) {

        val iterator = this.model.listObjectsOfProperty(subject.asResource(), getPropertyFromUri(predicateURI));
        while (iterator.hasNext()) {
            val data = iterator.next();
            if (data.isLiteral()) {
                return data.asLiteral().getString();
            }
        }
        return null;
    }

    List<String> getValuesOfDataProperty(final RDFNode subject,
                                         final String predicateURI,
                                         final Map<String, Object> args) {

        final List<String> valList = new ArrayList<>();

        val iterator = model.listObjectsOfProperty(subject.asResource(), getPropertyFromUri(predicateURI));

        while (iterator.hasNext()) {
            RDFNode data = iterator.next();
            if (data.isLiteral()) {
                if (!args.containsKey("lang") || args.get("lang").toString().equalsIgnoreCase(data.asLiteral().getLanguage())) {
                    valList.add(data.asLiteral().getString());
                }
            }
        }
        return valList;
    }

    List<RDFNode> getValuesOfObjectProperty(final String subjectURI, final String predicateURI) {
        return getValuesOfObjectProperty(getResourceFromUri(subjectURI), predicateURI);
    }

    List<RDFNode> getValuesOfObjectProperty(final RDFNode subject, final String predicateURI) {

        return getValuesOfObjectProperty(subject, predicateURI, null);
    }

    List<RDFNode> getValuesOfObjectProperty(final RDFNode subject, final String predicateURI, final String targetURI) {

        val iterator = this.model.listObjectsOfProperty(subject.asResource(), getPropertyFromUri(predicateURI));
        final List<RDFNode> rdfNodes = new ArrayList<>();
        iterator.forEachRemaining(node -> {
            if (!node.isLiteral()) {
                if (targetURI == null) {
                    rdfNodes.add(node);
                } else if (this.model.contains(node.asResource(), getPropertyFromUri(HGQLVocabulary.RDF_TYPE), getResourceFromUri(targetURI))) {
                    rdfNodes.add(node);
                }
            }
        });
        return rdfNodes;
    }

    RDFNode getValueOfObjectProperty(final RDFNode subject, final String predicateURI) {

        final List<RDFNode> values = getValuesOfObjectProperty(subject, predicateURI);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    void insertObjectTriple(final String subjectURI,
                            final String predicateURI,
                            final String objectURI) {
        model.add(getResourceFromUri(subjectURI), getPropertyFromUri(predicateURI), getResourceFromUri(objectURI));
    }

    void insertStringLiteralTriple(String subjectURI, String predicateURI, String value) {
        model.add(getResourceFromUri(subjectURI), getPropertyFromUri(predicateURI), value);
    }
}
