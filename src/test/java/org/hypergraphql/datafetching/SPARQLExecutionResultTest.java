package org.hypergraphql.datafetching;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import lombok.val;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SPARQLExecutionResultTest {

    @Test
    void toString_should_produce_intelligible_results() {

        val model = ModelFactory.createDefaultModel();
        model.add(
                model.createStatement(
                        model.createResource("http://data.hypergraphql.org/resources/123456"),
                        model.createProperty("http://data.hypergraphql.org/ontology/", "name"),
                        model.createLiteral("Test", "en")
                )
        );

        val result = new SPARQLExecutionResult(generateSimpleResultSet(), model);
        val toString = result.toString();

        val expectedToS = "RESULTS\nModel : \n"
                + "<ModelCom   {http://data.hypergraphql.org/resources/123456 "
                + "@http://data.hypergraphql.org/ontology/name \"Test\"@en} |  "
                + "[http://data.hypergraphql.org/resources/123456, http://data.hypergraphql.org/ontology/name, \"Test\"@en]>\n"
                + "ResultSet : \n{one=[1]}";

        assertEquals(expectedToS, toString);
    }

    private Map<String, Collection<String>> generateSimpleResultSet() {

        return Map.of("one", Set.of("1"));
    }
}
