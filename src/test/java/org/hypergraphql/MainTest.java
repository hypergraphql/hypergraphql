package org.hypergraphql;

import com.sun.xml.internal.bind.v2.runtime.property.PropertyFactory;
import org.apache.jena.rdf.model.*;

import java.util.Iterator;

public class MainTest {


    public static void main(String[] agrs) {

        Model model = ModelFactory.createDefaultModel();
        Resource subject = model.createResource("ciao");
        Property predicate = model.createProperty("ciao");
        RDFNode object = model.createLiteral("ciao");
        model.add(subject,predicate,object);
        Model model1 = ModelFactory.createDefaultModel();
        Resource subject1 = model1.createResource("ciao");
        Property predicate1 = model1.createProperty("ciao");
        Iterator<RDFNode> iterator = model.listObjectsOfProperty(predicate1);
        while (iterator.hasNext())
            System.out.println(iterator.next());



    }
}
