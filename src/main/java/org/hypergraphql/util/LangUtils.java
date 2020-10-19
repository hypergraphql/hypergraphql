package org.hypergraphql.util;

import org.apache.jena.riot.Lang;

public abstract class LangUtils {

    public static Lang forName(final String rdfFormat) {

        final String ucRdfFormat = rdfFormat.toUpperCase();
        switch (ucRdfFormat) {

            case "N3":
            case "TTL":
            case "TURTLE":
                return Lang.TURTLE;
            case "NT":
            case "NTRIPLES":
                return Lang.NTRIPLES;
            case "NQ":
            case "NQUADS":
                return Lang.NQUADS;
            case "TRIG":
                return Lang.TRIG;
            case "TRIX":
                return Lang.TRIX;
            case "RDFXML":
                return Lang.RDFXML;
            case "RDFJSON":
                return Lang.RDFJSON;
            case "JSONLD":
            case "JSON-LD":
            case "LD+JSON":
                return Lang.JSONLD;
            default:
                throw new RuntimeException(rdfFormat + " is not currently supported");
        }
    }
}
