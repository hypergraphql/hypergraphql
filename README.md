![HyperGraphQL](HyperGraphQL.png)  HyperGraphQL
======

This software has been developed by [Semantic Integration Ltd.](http://semanticintegration.co.uk) and is released under Apache License 2.0. See [LICENSE.TXT](LICENSE.TXT) for license infromation. 


## Summary

HyperGraphQL is a [GraphQL](http://graphql.org) query interface for RDF triple stores. It enables  querying of RDF stores via SPARQL endpoints using GraphQL query language and schemas mapped onto the target RDF vocabularies. 

HyperGraphQL serves two key objectives:

- hiding the complexities of the Semantic Web stack behind the GraphQL server, thus enabling access to linked data via a simpler and more familiar to many clients GraphQL interface;
- providing a flexible mechanism for restricting access to RDF stores down to naturally definable subsets of (tree-shaped) queries, which can be efficiently handled by the RDF stores, thus minimising the impact on the stores' availability. 

The responses of HyperGraphQL are [JSON-LD](http://json-ld.org) objects that convey full semantic context of the fetched data. This makes HyperGraphQL a natural [Linked Data Fragment](http://linkeddatafragments.org) interface for hypermedia-driven Web APIs backed by RDF stores. 


![HyperGraphQL-screenshot](screenshot.png) 


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


By deafault, the HyperGraphQL server starts at: 

```
http://localhost:8009
```

The [GraphiQL](https://github.com/graphql/graphiql) UI is initiated at:

```
http://localhost:8009/graphiql
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

- *schemaFile*: the file containing GraphQL schema definition;
- *contextFile*: the file containing mapping from the schema file to RDF vocabularies and respective SPARQL endpoints to be used for resolving GraphQL fields;
- *graphql.port*: the port at thich the GraphQL server and GraphiQL interface are initiated;
- *graphql.path*: the URL path of the GraphQL server;
- *graphql.graphiql*: the URL path of the GraphiQL UI.

## Example

The following query requests for a single person instance with its URI (_id) and RDF type (_type), its name, birthdate, and  birthplace with its URI, an english label, and the country in which it is located, also including its URI and an english label. 

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

The response of HyperGraphQL server to this query consists of the usual GraphQL JSON object, further augmented with a JSON-LD context, included as the value of the property "@context" on the "data" object.

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

It's easy to find out, using e.g. [JSON-LD playground](https://json-ld.org/playground/), that the "data" element in this response is in fact a valid JSON-LD object encoding the following RDF graph (in NTRIPLE notation):

```
_:b0 <http://hypergraphql/people> <http://dbpedia.org/resource/Sani_ol_molk> .
<http://dbpedia.org/resource/Sani_ol_molk> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Person> .
<http://dbpedia.org/resource/Sani_ol_molk> <http://xmlns.com/foaf/0.1/name> "Mirza Abolhassan Khan Ghaffari" .
<http://dbpedia.org/resource/Sani_ol_molk> <http://dbpedia.org/ontology/birthDate> "1814-1-1" .
<http://dbpedia.org/resource/Sani_ol_molk> <http://dbpedia.org/ontology/birthPlace> <http://dbpedia.org/resource/Kashan> .
<http://dbpedia.org/resource/Kashan> <http://www.w3.org/2000/01/rdf-schema#label> "Kashan" .
<http://dbpedia.org/resource/Kashan> <http://dbpedia.org/ontology/country> <http://dbpedia.org/resource/Iran> .
<http://dbpedia.org/resource/Iran> <http://www.w3.org/2000/01/rdf-schema#label> "Iran" .
```
This graph (except for the first triple, added by the HyperGraphQL service) is a subset of the DBpedia dataset. 

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

## RDF mapping

The RDF mapping consists of three components:

- *@predicates*: defining the URIs associated with the GraphQL vocabulary and the named graphs from which the data is to be fetched;
- *@namedGraphs*: defining the SPARQL endpoints where the named graphs are located;
- *@endpoints*: defining the URLs of the SPARQL endpoints and their authentication details. 

The following example presents a possible mapping for the schema above, where all predicates are associated with the *http://dbpedia.org* graph.

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

Note that: 
1) query fields (here: *people* and *cities*) are not mapped to URIs, but are nevertheless associated with some named graph;
2) object types, at least those that serve as output types of the query fields (here *Person* and *City*), must be associated with URIs, but not with the named graphs, as they will always be associated with the endpoint of the field;
3) each field of every object type must be associated with a URI and a named graph;
4) each named graph must be assoicated with a SPARQL endpoint;
5) each SPARQL endpoint must be accompanied by the authentication details. Whenever these are superfluous, they are asserted as empty strings.

HyperGraphQL supports also federated querying over a collection of SPARQL endpoints, although the current prototype implementation requires further optimizations. The federation is achieved by associating predicates with different SPARQL endpoints.  

## Query rewriting 

GraphQL queries are rewritten into SPARQL construct queries and executed against the designated SPARQL endpoints. 

All fields of the **Query** type are rewritten into instance subqueries, for intance:

```
{
people (limit:1, offset:6) {
  ...
}
}
```
is rewritten into:
```
{
  SELECT ?subject 
  WHERE {
    ?subject a <http://dbpedia.org/ontology/Person> .
  } LIMIT 1 OFFSET 6
}
...
```

Fields are translated into optional SPARQL triple patterns, additionally filtered with the RDF type associated with the output type of the field, provided such URI is specified in the mapping, for instance:
```
{
... {
  birthplace 
  ...
}
}
```
is rewritten:

```
...
OPTIONAL {
    ?subject <http://dbpedia.org/ontology/birthPlace> ?object .
    ?object a <http://dbpedia.org/ontology/City> .
    ...
}
```

## Execution

To minimise the number of return trips between HyperGraphQL server and RDF stores, the original GraphQL query is translated into possibly few SPARQL CONSTRUCT queries necessary to fetch all the relevant RDF data. The further transformation of the data into  HyperGraphQL responses is done locally by the HyperGraphQL server. When the query requests data from a single SPARQL endpoint, only one SPARQL CONSTRUCT query is issued. 

## Demo

A live demo of the HyperGraphQL server configured as in this repository is available at: [hypergraphql-dbpedia](http://104.154.59.211:8009/graphiql)

## Contact

Email: **szymon.klarman@semanticintegration.co.uk**
