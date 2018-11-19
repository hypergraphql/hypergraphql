package org.hypergraphql.datafetching;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SPARQLExecutionResultTest {

    @Test
    void toString_should_produce_intelligible_results() {

        final Model model = ModelFactory.createDefaultModel();
        model.add(
                model.createStatement(
                        model.createResource("http://data.hypergraphql.org/resources/123456"),
                        model.createProperty("http://data.hypergraphql.org/ontology/", "name"),
                        model.createLiteral("Test", "en")
                )
        );

        final SPARQLExecutionResult result = new SPARQLExecutionResult(generateSimpleResultSet(), model);
        final String toString = result.toString();

        final String expectedToS = "RESULTS\nModel : \n" +
                "<ModelCom   {http://data.hypergraphql.org/resources/123456 " +
                "@http://data.hypergraphql.org/ontology/name \"Test\"@en} |  " +
                "[http://data.hypergraphql.org/resources/123456, http://data.hypergraphql.org/ontology/name, \"Test\"@en]>\n" +
                "ResultSet : \n{one=[1]}";

        assertEquals(expectedToS, toString);
    }

    private Map<String, Set<String>> generateSimpleResultSet() {

        return new HashMap<String, Set<String>>() {{
            put("one", new HashSet<String>() {{ add("1"); }});
        }};
    }
}