package org.hypergraphql.datamodel;

import graphql.language.ListType;
import graphql.language.NonNullType;
import graphql.language.Type;
import graphql.language.TypeName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HGQLSchemaTest {


    @Test
    void equality_for_class_type() {

        final Type typeName = new TypeName("typeName");

        final Type listType = new ListType();

        final Type nonNullType = new NonNullType();

        assertTrue(typeName.getClass() == TypeName.class);
        assertTrue(typeName instanceof TypeName);
    }
}