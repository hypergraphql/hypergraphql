![HyperGraphQL](HyperGraphQL.png)  HyperGraphQL
======

HyperGraphQL is a [GraphQL](http://graphql.org) query interface for RDF triple stores. It enables  querying of RDF stores via SPARQL endpoints using GraphQL query language and schemas mapped onto the target RDF vocabularies. 

HyperGraphQL serves two key objectives:

- hiding the complexities of the Semantic Web stack behind the GraphQL server, thus enabling access to linked data via a simpler and more familiar to many clients GraphQL interface;
- providing a flexible mechanism for restricting access to RDF stores down to naturally definable subsets of (tree-shaped) queries, which can be efficiently handled by the RDF stores, thus minimising the impact on the stores' availability. 

The responses of HyperGraphQL are [JSON-LD](http://json-ld.org) objects that convey full semantic context of the fetched data. This makes HyperGraphQL a natural [Linked Data Fragment](http://linkeddatafragments.org) interface for hypermedia-driven Web APIs backed by RDF stores. 

## GraphQL schema + RDF mapping = HyperGraphQL server

To configure a HyperGraphQL server one needs to provide only a basic GraphQL type schema and its mapping to the target RDF vocabulary and SPARQL endpoints. The complete GraphQL wiring is done automatically on initiating the server. 

## Running

Clone the Git repository into a local directory. Then in the root of the project execute the following: 

**Maven**: 
1) **mvn install**
2) **mvn exec:java**

(*Note*: in Windows these must be executed in a *cmd* terminal, not *PowerShell*).

**Gradle**: 
1) **gradle build**
2) **gradle execute**

## Example

### HyperGraphQL query:
```
{
  people(limit: 1, offset: 6) {
    _id
    _type
    name
    birthDate
    birthPlace {
      _id
      label(lang: "en")
      country {
        _id
        label(lang: "en")
      }
    }
  }
}
```

### HyperGraphQL response:
```js
{
  "extensions": {},
  "data": {
    "people": [
      {
        "_id": "http://dbpedia.org/resource/Sani_ol_molk",
        "_type": "http://dbpedia.org/ontology/Person",
        "name": "Mirza Abolhassan Khan Ghaffari",
        "birthDate": "1814-1-1",
        "birthPlace": {
          "_id": "http://dbpedia.org/resource/Kashan",
          "label": [
            "Kashan"
          ],
          "country": {
            "_id": "http://dbpedia.org/resource/Iran",
            "label": [
              "Iran"
            ]
          }
        }
      }
    ],
    "@context": {
      "birthPlace": "http://dbpedia.org/ontology/birthPlace",
      "country": "http://dbpedia.org/ontology/country",
      "_type": "@type",
      "name": "http://xmlns.com/foaf/0.1/name",
      "_id": "@id",
      "label": "http://www.w3.org/2000/01/rdf-schema#label",
      "people": "http://hypergraphql/people",
      "birthDate": "http://dbpedia.org/ontology/birthDate"
    }
  },
  "errors": []
}
```

## Properties

Basic settings are defined in the *properties.json* file. The defaults are:

```js
{
    "schemaFile": "schema.graphql",
    "contextFile": "context.json",
    "graphql": {
        "port": 8009,
        "path": "/",
        "graphiql": "/graphiql"
    }
}
```

- *schemaFile*: the file containing GraphQL schema definition
- *contextFile*: the file containing mapping from the schema file to RDF vocabularies and respective SPARQL endpoints to be used for resolving GraphQL fields
- *graphql.port*: the port number at thich the GraphQL server and GraphiQL interface are initiated
- *graphql.path*: the relative URL of the GraphQL server
- *graphql.graphiql*: the relative URL of the [GraphiQL UI](https://github.com/graphql/graphiql)

## Schema

The schema definition complies with the GraphQL spec (see: 	[http://graphql.org/learn/schema/](http://graphql.org/learn/schema/)). Currently, only the core fragment of the spec, including object types and fields, is supported, as presented in the example below. Additionally, some default SPARQL-related fields and arguments are added automatically to each schema. 


```
type Query {
    people: [Person]
    cities: [City]
}

type Person {
    name: [String]
    label: [String]
    birthPlace: City
    birthDate: String
}

type City {
    label: [String]
    country: Country
    leader: Person
}

type Country {
    label: [String]
}
```

All fields of the **Query** type are effectively translated into instance queries of the form:
```
SELECT ?subject 
WHERE {
    ?subject a <URI(field)> .
}
```
where *URI(field)* denotes the URI associated with the *field* name.

All other fields are translated into object queries where the field name is associated with the URI of a predicate in the triple pattern, i.e.:

```
SELECT ?object 
WHERE {
    ?parent <URI(field)> ?object .
}
```

## Context

The context specification consists of three components:

- *@predicates*: defining the URI associated with every field name and the RDF graph id where the data is to be fetched;
- *@namedGraphs*: defining the RDF graph ids are associated with their full names and the SPARQL endpoints in which they are located;
- *@endpoints*: defining the URL associated with each endpoint and possibly the authentication details. 

The following example presents a possible context associated with the schema above, where all predicates are associated with the *http://dbpedia.org* graph.

```js
{
  "@predicates": {
    "people": {
      "@namedGraph": "dbpedia"
    },
    "cities": {
      "@namedGraph": "dbpedia"
    },
    "Person": {
      "@id": "http://dbpedia.org/ontology/Person"
    },
    "City": {
      "@id": "http://dbpedia.org/ontology/City"
    },
    "name": {
      "@id": "http://xmlns.com/foaf/0.1/name",
      "@namedGraph": "dbpedia"
    },
    "birthDate": {
      "@id": "http://dbpedia.org/ontology/birthDate",
      "@namedGraph": "dbpedia"
    },
    "birthPlace": {
      "@id": "http://dbpedia.org/ontology/birthPlace",
      "@namedGraph": "dbpedia"
    },
    "label": {
      "@id": "http://www.w3.org/2000/01/rdf-schema#label",
      "@namedGraph": "dbpedia"
    },
    "country": {
      "@id": "http://dbpedia.org/ontology/country",
      "@namedGraph": "dbpedia"
    },
    "leader": {
      "@id": "http://dbpedia.org/ontology/leaderName",
      "@namedGraph": "dbpedia"
    }
  },
  "@namedGraphs": {
    "dbpedia": {
      "@id": "http://dbpedia.org",
      "@endpoint": "dbpedia-endpoint"
    }
  },
  "@endpoints": {
    "dbpedia-endpoint": {
      "@id": "http://dbpedia.org/sparql/",
      "@user": "",
      "@password": ""
    }
  }
}
```

Note that HyperGraphQL supports also federated querying over a collection of SPARQL endpoints, although the current prototype implementation requires further optimizations. The federation is achieved by associating predicates with different SPARQL endpoints.  

## Execution

To minimise the number of return trips between HyperGraphQL server and RDF stores, the original GraphQL query is translated into possibly few SPARQL CONSTRUCT queries necessary to fetch all the relevant RDF data. The further transformation of the data into  HyperGraphQL responses is done locally by the HyperGraphQL server. When the query requests data from a single SPARQL endpoint, only one SPARQL CONSTRUCT query is issued. 

## Demo

A live demo of the HyperGraphQL server configured as in this repository is available at: [http://hypergraphql-demo/graphiql](http://104.154.59.211:8009/graphiql)

## License

This software is released under the Apache 2.0 license. See [LICENSE.TXT](LICENSE.TXT) for license details. 


## Contact

Email: [szymon.klarman@semanticintegration.co.uk](szymon.klarman@semanticintegration.co.uk)
