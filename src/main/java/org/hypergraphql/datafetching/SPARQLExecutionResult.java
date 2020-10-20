package org.hypergraphql.datafetching;

import java.util.Collection;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.jena.rdf.model.Model;

@Getter
@RequiredArgsConstructor
public class SPARQLExecutionResult {

    private final Map<String, Collection<String>> resultSet;
    private final Model model;

    @Override
    public String toString() {

        return "RESULTS\n"
                + "Model : \n" + this.model.toString() + "\n"
                + "ResultSet : \n" + this.resultSet.toString();
    }
}
