package org.hypergraphql.datafetching.services;



import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;

import org.apache.jena.sparql.core.DatasetGraph;
import org.hypergraphql.Controller;
import org.hypergraphql.config.system.HGQLConfig;
import org.hypergraphql.datamodel.HGQLSchemaWiring;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;


class HGraphQLServiceTest {

    @Test
    public void getModelFromHGQLService() throws IllegalAccessException, NoSuchFieldException, NoSuchMethodException, InvocationTargetException {


        HGQLConfig config = new HGQLConfig("src/test/resources/config.json");
        Controller controller = new Controller();
        controller.start(config);


        HGraphQLService hgqlService = new HGraphQLService();

        Field url = HGraphQLService.class.getDeclaredField("url");
        url.setAccessible(true);
        url.set(hgqlService, "http://localhost:" + config.getGraphqlConfig().port() + config.getGraphqlConfig().graphqlPath());

        Field user = HGraphQLService.class.getDeclaredField("user");
        user.setAccessible(true);
        user.set(hgqlService, "");

        Field password = HGraphQLService.class.getDeclaredField("password");
        password.setAccessible(true);
        password.set(hgqlService, "");

        String testQuery = "" +
                "{\n" +
                "  Person_GET(limit:10) {\n" +
                "    name\n" +
                "    _id\n" +
                "    birthDate\n" +
                "    birthPlace {\n" +
                "      label(lang:\"en\")\n" +
                "      country {\n" +
                "        _id\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";

        Method method = HGraphQLService.class.getDeclaredMethod("getModelFromRemote", String.class);
        method.setAccessible(true);
        Model model = (Model) method.invoke(hgqlService, testQuery);

        assertTrue(model.size()>10);

    }

    }