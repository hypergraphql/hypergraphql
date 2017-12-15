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
    <script type="application/javascript" src="https://semantic-integration.github.io/hypergraphql/sources/graphiqlinit.js"></script>
</graphiqlconfig>



In this tutorial we will demonstrate the end-to-end process of defining and linking several linked data services and datasets using HyperGraphQL.

<img src="https://semantic-integration.github.io/hypergraphql/sources/service-linking.svg" alt="diagram">


## Service 1: DBpedia SPARQL endpoint

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
            "type": "LocalModelService",
            "filepath": "agrovoc.ttl",
            "filetype": "TTL"
        }
    ]
}
```

```
type __Context {
    Scheme:         _@href(iri: "http://www.w3.org/2004/02/skos/core#ConceptScheme")
    Concept:        _@href(iri: "http://www.w3.org/2004/02/skos/core#Concept")
    hasTopConcept:        _@href(iri: "http://www.w3.org/2004/02/skos/core#hasTopConcept")
    prefLabel:        _@href(iri: "http://www.w3.org/2004/02/skos/core#prefLabel")
    altLabel:        _@href(iri: "http://www.w3.org/2004/02/skos/core#altLabel")
    hiddenLabel:        _@href(iri: "http://www.w3.org/2004/02/skos/core#hiddenLabel")
    broader:        _@href(iri: "http://www.w3.org/2004/02/skos/core#broader")
    narrower:        _@href(iri: "http://www.w3.org/2004/02/skos/core#narrower")
    related:        _@href(iri: "http://www.w3.org/2004/02/skos/core#related")
    broadMatch:        _@href(iri: "http://www.w3.org/2004/02/skos/core#broadMatch")
    closeMatch:        _@href(iri: "http://www.w3.org/2004/02/skos/core#closeMatch")
    narrowMatch:        _@href(iri: "http://www.w3.org/2004/02/skos/core#narrowMatch")
    relatedMatch:        _@href(iri: "http://www.w3.org/2004/02/skos/core#relatedMatch")
    exactMatch:        _@href(iri: "http://www.w3.org/2004/02/skos/core#exactMatch")
    note:        _@href(iri: "http://www.w3.org/2004/02/skos/core#note")
    definition:        _@href(iri: "http://www.w3.org/2004/02/skos/core#definition")
    inScheme:        _@href(iri: "http://www.w3.org/2004/02/skos/core#inScheme")
    topConceptOf:        _@href(iri: "http://www.w3.org/2004/02/skos/core#topConceptOf")
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
    }
    narrower {
      _id
      prefLabel(lang:"en")
    }
    related {
      _id
      prefLabel(lang:"en")
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
            "type": "LocalModelService",
            "filepath": "fao.ttl",
            "filetype": "TTL"
        }
    ]
}
```

```
type __Context {
    City:           _@href(iri: "http://dbpedia.org/ontology/City")
    Country:        _@href(iri: "http://dbpedia.org/ontology/Country")
    label:          _@href(iri: "http://www.w3.org/2000/01/rdf-schema#label")
    comment:        _@href(iri: "http://www.w3.org/2000/01/rdf-schema#comment")
    country:        _@href(iri: "http://dbpedia.org/ontology/country")
    FaoCountry:     _@href(iri: "http://www.fao.org/countryprofiles/geoinfo/geopolitical/resource/self_governing")
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
    faoName:        _@href(iri: "http://www.fao.org/countryprofiles/geoinfo/geopolitical/resource/nameOfficial")
    population:     _@href(iri: "http://www.fao.org/countryprofiles/geoinfo/geopolitical/resource/populationTotal")

}

type FaoCountry @service(id:"fao-local") {
    faoName: [String] @service(id:"fao-local")
    hasBorderWith: FaoCountry @service(id:"fao-local")
    sameInDBpedia: Country @service(id:"fao-local")
    sameInAgrovoc: Concept @service(id:"fao-local")
    inEconomicRegion: [EconomicRegion] @service(id:"fao-local")
    inGeoRegion: [GeoRegion] @service(id:"fao-local")
    population: Int @service(id:"fao-local")
}

type EconomicRegion @service(id:"fao-local") {
    faoName: [String] @service(id:"fao-local")
    sameInAgrovoc: Concept @service(id:"fao-local")
    hasMemberCountry: [FaoCountry] @service(id:"fao-local")
}

type GeoRegion @service(id:"fao-local") {
    faoName: [String] @service(id:"fao-local")
    sameInAgrovoc: Concept @service(id:"fao-local")
    hasMemberCountry: [FaoCountry] @service(id:"fao-local")
}

type City {
    label: [String] @service(id:"dbpedia-hgql")
    country: Country @service(id:"dbpedia-hgql")
    comment: [String] @service(id:"dbpedia-hgql")
}

type Country {
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

<graphiql id="tutorial3" graphql="graphql3" graphiql="graphiql3" query=
'{
  FaoCountry_GET_BY_ID(uris:[
    "http://www.fao.org/countryprofiles/geoinfo/geopolitical/resource/Afghanistan"
  ]) {
    _id
    _type
    faoName(lang:"en")
    population
    hasBorderWith {
      _id
      faoName(lang:"en")
    }
    inGeoRegion {
      _id
      faoName(lang:"en")
    }
    inEconomicRegion {
      _id
      faoName(lang:"en")
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