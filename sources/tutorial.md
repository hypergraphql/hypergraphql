---
layout: default
title: Tutorial
permalink: /tutorial/
---

<graphiqlconfig>
    <script src="//cdn.jsdelivr.net/es6-promise/4.0.5/es6-promise.auto.min.js"></script>
    <script src="//cdn.jsdelivr.net/fetch/0.9.0/fetch.min.js"></script>
    <script src="//cdn.jsdelivr.net/react/15.4.2/react.min.js"></script>
    <script src="//cdn.jsdelivr.net/react/15.4.2/react-dom.min.js"></script>
    <link rel="stylesheet" href="//cdn.jsdelivr.net/npm/graphiql@0.11.2/graphiql.css" />
    <script src="//cdn.jsdelivr.net/npm/graphiql@0.11.2/graphiql.js"></script>
    <script type="application/javascript" src="/scripts/graphiqlinit.js"></script>
    <script type="application/javascript" src="/scripts/homepage-server.js"></script>
</graphiqlconfig>


# Tutorial

In this tutorial we outline all the steps required to define and connect several linked data services and datasets using HyperGraphQL. The resulting architecture consists of three HyperGraphQL instances:
1. an instance exposing a fragment of [DBpedia](http://wiki.dbpedia.org/) dataset accessed via the SPARQL endpoint of DBpedia;
2. an instance serving the [AGROVOC SKOS taxonomy](http://aims.fao.org/vest-registry/vocabularies/agrovoc-multilingual-agricultural-thesaurus) uploaded from a local file;
3. an instance serving the [FAO Geopolitical Ontology](http://www.fao.org/countryprofiles/geoinfo/geopolitical/resource/) uploaded from a local file, containing `owl:sameAs` links to RDF resources exposed by the other two HyperGraphQL instances. 


<img src="https://semantic-integration.github.io/hypergraphql/sources/service-linking.png" alt="diagram">

All the resources, including configuration files, GraphQL schemas and RDF datasets, are included in the [src/test/resources/demo_services](https://github.com/semantic-integration/hypergraphql/tree/master/src/test/resources/demo_services) directory of the project's GitHub repository. 

In order to start all three instances described in this tutorial at once, you can run the `ClasspathDemo` or `Demo` executable classes in your local repository of HyperGraphQL in the `test` source tree (within the `demo` package).

These effectively spin up several instances of HyperGraphQL pointing at different sources:

```java
HGQLConfig config1 = HGQLConfig.fromClasspathConfig("demo_services/config1.json");
new Controller().start(config1); //dbpedia-hgql
HGQLConfig config2 = HGQLConfig.fromClasspathConfig("demo_services/config2.json");
new Controller().start(config2); //agrovoc-hgql
HGQLConfig config3 = HGQLConfig.fromClasspathConfig("demo_services/config3.json");
new Controller().start(config3); //fao-go-hgql
```

<br>

## Service 1: DBpedia SPARQL endpoint

The HyperGraphQL instance querying the DBpedia SPARQL endpoint is defined exactly as described in the [Documentation](/documentation) section and initiated on port 8081. Notably, the service type is `SPARQLEndpointService` and the endpoint's URL is set to `http://dbpedia.org/sparql/`.

```json
{
    "name": "dbpedia-hgql",
    "schema": "schema1.graphql",
    "server": {
        "port": 8081,
        "graphql": "/graphql",
        "graphiql": "/graphiql"
    },
    "services": [
        {
            "id": "dbpedia-sparql",
            "type": "SPARQLEndpointService",
            "url": "http://dbpedia.org/sparql/",
            "graph": "http://dbpedia.org",
            "user": "",
            "password": ""
        }
    ]
}
```

<br>

The schema is focused around just two types - `Country` and `City` - with a few basic properties. Note that all types/field are associated with unique IRIs in the `__Context` type.


```
type __Context {
    City:           _@href(iri: "http://dbpedia.org/ontology/City")
    Country:        _@href(iri: "http://dbpedia.org/ontology/Country")
    label:          _@href(iri: "http://www.w3.org/2000/01/rdf-schema#label")
    comment:        _@href(iri: "http://www.w3.org/2000/01/rdf-schema#comment")
    country:        _@href(iri: "http://dbpedia.org/ontology/country")
    capital:        _@href(iri: "http://dbpedia.org/ontology/capital")
}

type City @service(id:"dbpedia-sparql") {
    label: [String] @service(id:"dbpedia-sparql")
    country: Country @service(id:"dbpedia-sparql")
    comment: [String] @service(id:"dbpedia-sparql")
}

type Country @service(id:"dbpedia-sparql") {
    label: [String] @service(id:"dbpedia-sparql")
    capital: City @service(id:"dbpedia-sparql")
    comment: [String] @service(id:"dbpedia-sparql")
}
```

<br>

The result of setting up this instance correctly should be a GraphiQL interface as below:


<graphiql id="tutorial1" graphql="graphql1" graphiql="graphiql1" query=
'{
  Country_GET_BY_ID(uris:[
      "http://dbpedia.org/resource/Democratic_Republic_of_Afghanistan", 
      "http://dbpedia.org/resource/Afghanistan"
      ]) {
    _id
    _type
    comment(lang:"en")
    label(lang:"en")
    capital {
      _id
      label(lang:"en")
      comment(lang:"en")
      country {
        _id
      }
    }
  }
}'
>
    <script>
       graphiqlInit('tutorial1');
    </script>
</graphiql>

<br>

## Service 2: AGROVOC taxonomy file

Next, we define another instance which is used to access the AGROVOC SKOS taxonomy, originally uploaded from a local file. The file `agrovoc.ttl`, used here, contains RDF data in the turtle format, which is a subset of the original AGROVOC dataset expressed exclusively in the [SKOS vocabulary](https://www.w3.org/TR/skos-reference/). In the configuration file, the service type is set to `LocalModelSPARQLService` and the instance is initiated on port 8082. 

```json
{
    "name": "agrovoc-hgql",
    "schema": "schema2.graphql",
    "server": {
        "port": 8082,
        "graphql": "/graphql",
        "graphiql": "/graphiql"
    },
    "services": [
        {
            "id": "agrovoc-local",
            "type": "LocalModelSPARQLService",
            "filepath": "agrovoc.ttl",
            "filetype": "TTL"
        }
    ]
}
```

<br>

Note that the schema below is in fact SKOS-generic, in the sense that it is suitable for exposing any SKOS taxonomy, not only AGROVOC. However, since HyperGraphQL is not able to infer any new relationships by its own, it has to be ensured that the relevant inferences are materialized in advance. For instance, since the AGROVOC taxonomy originally does not contain any `skos:narrower` relationships (only their inverses, `skos:broader`, are present) these have to be first included explicitly in the RDF graph.


```
type __Context {
    Scheme:         _@href(iri: "http://www.w3.org/2004/02/skos/core#ConceptScheme")
    Concept:        _@href(iri: "http://www.w3.org/2004/02/skos/core#Concept")
    hasTopConcept:  _@href(iri: "http://www.w3.org/2004/02/skos/core#hasTopConcept")
    prefLabel:      _@href(iri: "http://www.w3.org/2004/02/skos/core#prefLabel")
    altLabel:       _@href(iri: "http://www.w3.org/2004/02/skos/core#altLabel")
    hiddenLabel:    _@href(iri: "http://www.w3.org/2004/02/skos/core#hiddenLabel")
    broader:        _@href(iri: "http://www.w3.org/2004/02/skos/core#broader")
    narrower:       _@href(iri: "http://www.w3.org/2004/02/skos/core#narrower")
    related:        _@href(iri: "http://www.w3.org/2004/02/skos/core#related")
    broadMatch:     _@href(iri: "http://www.w3.org/2004/02/skos/core#broadMatch")
    closeMatch:     _@href(iri: "http://www.w3.org/2004/02/skos/core#closeMatch")
    narrowMatch:    _@href(iri: "http://www.w3.org/2004/02/skos/core#narrowMatch")
    relatedMatch:   _@href(iri: "http://www.w3.org/2004/02/skos/core#relatedMatch")
    exactMatch:     _@href(iri: "http://www.w3.org/2004/02/skos/core#exactMatch")
    note:           _@href(iri: "http://www.w3.org/2004/02/skos/core#note")
    definition:     _@href(iri: "http://www.w3.org/2004/02/skos/core#definition")
    inScheme:       _@href(iri: "http://www.w3.org/2004/02/skos/core#inScheme")
    topConceptOf:   _@href(iri: "http://www.w3.org/2004/02/skos/core#topConceptOf")
}

type Scheme @service(id:"agrovoc-local") {
    hasTopConcept: [Concept] @service(id:"agrovoc-local")
    prefLabel: [String] @service(id:"agrovoc-local")
  }

type Concept @service(id:"agrovoc-local") {
    prefLabel: [String] @service(id:"agrovoc-local")
    altLabel: [String] @service(id:"agrovoc-local")
    hiddenLabel: [String] @service(id:"agrovoc-local")
    broader: [Concept] @service(id:"agrovoc-local")
    narrower: [Concept] @service(id:"agrovoc-local")
    related: [Concept] @service(id:"agrovoc-local")
    broadMatch: [Concept] @service(id:"agrovoc-local")
    closeMatch: [Concept] @service(id:"agrovoc-local")
    narrowMatch: [Concept] @service(id:"agrovoc-local")
    relatedMatch: [Concept] @service(id:"agrovoc-local")
    exactMatch: [Concept] @service(id:"agrovoc-local")
    note: [String] @service(id:"agrovoc-local")
    definition: [String] @service(id:"agrovoc-local")
    inScheme: [Scheme] @service(id:"agrovoc-local")
    topConceptOf: [Scheme] @service(id:"agrovoc-local")
}
```

<br>

As a result, the GraphiQL interface exposes fields intended for querying SKOS concepts, their relationships and labels, as shown below.


<graphiql id="tutorial2" graphql="graphql2" graphiql="graphiql2" query=
'{
  Concept_GET_BY_ID(uris:[
    "http://aims.fao.org/aos/agrovoc/c_163"
  ]) {
    _id
    prefLabel(lang:"en")
    altLabel(lang:"en")
    broader {
      _id
      prefLabel(lang:"en")
        narrower {
        _id
        prefLabel(lang:"en")
      }
    }
  }
}'
>
    <script>
       graphiqlInit('tutorial2');
    </script>
</graphiql>


<br>

## Service 3: Geopolitical Ontology file + HGQL services 1 & 2

Finally, we define the last HyperGraphQL instance, which employs some supplementary RDF data from the Geopolitical Ontology and connects all three data sources together. In the configuration file we include references to both previously defined HyperGraphQL instances of type `HGraphQLService` and exposed with URLs `http://localhost:8081/graphql` and `http://localhost:8082/graphql`, respectively. The third service, of type `LocalModelSPARQLService`, is specified analogically to the case of AGROVOC, using a reference to a local file `fao.ttl`, again in turtle format. The instance is initiated on port 8083.


```json
{
    "name": "fao-go-hgql",
    "schema": "schema3.graphql",
    "server": {
        "port": 8083,
        "graphql": "/graphql",
        "graphiql": "/graphiql"
    },
    "services": [
        {
            "id": "dbpedia-hgql",
            "type": "HGraphQLService",
            "url": "http://localhost:8081/graphql"
        },
        {
            "id": "agrovoc-hgql",
            "type": "HGraphQLService",
            "url": "http://localhost:8082/graphql"
        },
        {
            "id": "fao-local",
            "type": "LocalModelSPARQLService",
            "filepath": "fao.ttl",
            "filetype": "TTL"
        }
    ]
}
```

<br>

The schema directly reuses certain parts of the schemas of the previously defined HyperGraphQL instances, e.g., `City`, `Country` and `Concept`, with selected subsets of their original fields. It is crucial that the labels and IRIs of such reused parts match exactly those from the original HyperGraphQL instances, as this warrants correct linking of services and semantically faithful communication between them. Parts of the original schemas that are not relevant for the current service can be safely skipped. Also, whenever a type from a target HyperGraphQL service is to be used in the current schema, but not made directly queryable, its corresponding `@service(id:"...")` annotation should be skipped, as it is done below in the case of `City`, `Country` and `Concept`.


```
type __Context {
    City:           _@href(iri: "http://dbpedia.org/ontology/City")
    Country:        _@href(iri: "http://dbpedia.org/ontology/Country")
    label:          _@href(iri: "http://www.w3.org/2000/01/rdf-schema#label")
    comment:        _@href(iri: "http://www.w3.org/2000/01/rdf-schema#comment")
    country:        _@href(iri: "http://dbpedia.org/ontology/country")
    capital:        _@href(iri: "http://dbpedia.org/ontology/capital")
    SelfGoverning:  _@href(iri: "http://www.fao.org/countryprofiles/geoinfo/geopolitical/resource/self_governing")
    hasBorderWith:  _@href(iri: "http://www.fao.org/countryprofiles/geoinfo/geopolitical/resource/hasBorderWith")
    GeoRegion:      _@href(iri: "http://www.fao.org/countryprofiles/geoinfo/geopolitical/resource/geographical_region")
    EconomicRegion: _@href(iri: "http://www.fao.org/countryprofiles/geoinfo/geopolitical/resource/economic_region")
    inEconomicRegion: _@href(iri: "http://www.fao.org/countryprofiles/geoinfo/geopolitical/resource/isInGroup")
    inGeoRegion:    _@href(iri: "http://www.fao.org/countryprofiles/geoinfo/geopolitical/resource/isInGroup")
    hasMemberCountry: _@href(iri: "http://www.fao.org/countryprofiles/geoinfo/geopolitical/resource/hasMember")
    Concept:        _@href(iri: "http://www.w3.org/2004/02/skos/core#Concept")
    prefLabel:      _@href(iri: "http://www.w3.org/2004/02/skos/core#prefLabel")
    altLabel:       _@href(iri: "http://www.w3.org/2004/02/skos/core#altLabel")
    broader:        _@href(iri: "http://www.w3.org/2004/02/skos/core#broader")
    narrower:       _@href(iri: "http://www.w3.org/2004/02/skos/core#narrower")
    related:        _@href(iri: "http://www.w3.org/2004/02/skos/core#related")
    sameInDBpedia:  _@href(iri: "http://www.w3.org/2002/07/owl#sameAs")
    sameInAgrovoc:  _@href(iri: "http://www.w3.org/2002/07/owl#sameAs")
    name:           _@href(iri: "http://www.fao.org/countryprofiles/geoinfo/geopolitical/resource/nameOfficial")
    population:     _@href(iri: "http://www.fao.org/countryprofiles/geoinfo/geopolitical/resource/populationTotal")

}

type SelfGoverning @service(id:"fao-local") {
    name: [String] @service(id:"fao-local")
    hasBorderWith: [SelfGoverning] @service(id:"fao-local")
    sameInDBpedia: Country @service(id:"fao-local")
    sameInAgrovoc: Concept @service(id:"fao-local")
    inEconomicRegion: [EconomicRegion] @service(id:"fao-local")
    inGeoRegion: [GeoRegion] @service(id:"fao-local")
    population: Int @service(id:"fao-local")
}

type EconomicRegion @service(id:"fao-local") {
    name: [String] @service(id:"fao-local")
    sameInAgrovoc: Concept @service(id:"fao-local")
    hasMemberCountry: [SelfGoverning] @service(id:"fao-local")
}

type GeoRegion @service(id:"fao-local") {
    name: [String] @service(id:"fao-local")
    sameInAgrovoc: Concept @service(id:"fao-local")
    hasMemberCountry: [SelfGoverning] @service(id:"fao-local")
}

type City {
    label: [String] @service(id:"dbpedia-hgql")
    country: Country @service(id:"dbpedia-hgql")
    comment: [String] @service(id:"dbpedia-hgql")
}

type Country  {
    label: [String] @service(id:"dbpedia-hgql")
    capital: [City] @service(id:"dbpedia-hgql")
    comment: [String] @service(id:"dbpedia-hgql")
}

type Concept {
    prefLabel: [String] @service(id:"agrovoc-hgql")
    altLabel: [String] @service(id:"agrovoc-hgql")
    broader: [Concept] @service(id:"agrovoc-hgql")
    narrower: [Concept] @service(id:"agrovoc-hgql")
    related: [Concept] @service(id:"agrovoc-hgql")
}
```

<br>

The Geopolitical Ontology contains outgoing `owl:sameAs` links to both DBpedia and AGROVOC taxonomy. These links are critical to connecting the three datasets, and are captured in the schema within the type `SelfGoverning` (roughly corresponding to "Country" in the Geopolitical Ontology), which includes fields `sameInDBpedia` and `sameInAgrovoc`, both mapped on the IRI `owl:sameAs`. Using these links one can naturally construct queries seamlessly spanning all three datasets and HyperGraphQL instances. Note, that since currently each instance applies individually strict type checking on the values of the fields, the Geopolitical Ontology must be first extended with the missing `rdf:type` assertions over the objects of `owl:sameAs` property. Consequently, each DBpedia identifier of a country must be asserted as an instance of `dbo:Country` and each AGROVOC identifier as an instance of `skos:Concept`. This restriction should be likely relaxed in the future versions of HyperGraphQL. 

The working example of this instance is embedded below.



<graphiql id="tutorial3" graphql="graphql3" graphiql="graphiql3" query=
'{
  SelfGoverning_GET_BY_ID(uris:[
    "http://www.fao.org/countryprofiles/geoinfo/geopolitical/resource/Afghanistan"
  ]) {
    _id
    _type
    name(lang:"en")
    population
    hasBorderWith {
      _id
    }
    inEconomicRegion {
      _id
      name(lang:"en")
    }
    sameInAgrovoc {
      _id
      prefLabel(lang:"en")
      broader {
        _id
        prefLabel(lang:"en")
      }
    }
    sameInDBpedia {
      _id
      label(lang:"en")
      comment(lang:"en")
      capital {
        _id
        label(lang:"en")
        comment(lang:"en")
      }
    }
  }
}'
>
    <script>
       graphiqlInit('tutorial3');
    </script>
</graphiql>
<br>
