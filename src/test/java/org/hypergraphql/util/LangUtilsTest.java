package org.hypergraphql.util;

import lombok.val;
import org.apache.jena.riot.Lang;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LangUtilsTest {

    @Test
    void should_accept_turtle() {

        assertEquals(Lang.TURTLE, LangUtils.forName("ttl"));
        assertEquals(Lang.TURTLE, LangUtils.forName("TTL"));
        assertEquals(Lang.TURTLE, LangUtils.forName("turtle"));
        assertEquals(Lang.TURTLE, LangUtils.forName("TURTLE"));
        assertEquals(Lang.TURTLE, LangUtils.forName("n3"));
        assertEquals(Lang.TURTLE, LangUtils.forName("N3"));
    }

    @Test
    void should_accept_trig() {

        assertEquals(Lang.TRIG, LangUtils.forName("trig"));
    }

    @Test
    void should_accept_trix() {

        assertEquals(Lang.TRIX, LangUtils.forName("TRiX"));
    }

    @Test
    void should_accept_jsonld() {

        assertEquals(Lang.JSONLD, LangUtils.forName("jsonld"));
        assertEquals(Lang.JSONLD, LangUtils.forName("json-ld"));
        assertEquals(Lang.JSONLD, LangUtils.forName("ld+json"));
    }

    @Test
    void should_accept_ntriples() {
        assertEquals(Lang.NTRIPLES, LangUtils.forName("NT"));
        assertEquals(Lang.NTRIPLES, LangUtils.forName("ntriples"));
    }

    @Test
    void should_accept_nquads() {
        assertEquals(Lang.NQUADS, LangUtils.forName("NQ"));
        assertEquals(Lang.NQUADS, LangUtils.forName("nquads"));
    }

    @Test
    void should_accept_rdfxml() {
        assertEquals(Lang.RDFXML, LangUtils.forName("RDFXML"));
    }

    @Test
    void should_accept_rdfjson() {
        assertEquals(Lang.RDFJSON, LangUtils.forName("RDFJSON"));
    }

    @Test
    void should_reject_unknown_type() {

        val message = "GremLin is not currently supported";
        val thrown = assertThrows(RuntimeException.class, () -> LangUtils.forName("GremLin"));
        assertEquals(message, thrown.getMessage());
    }
}
