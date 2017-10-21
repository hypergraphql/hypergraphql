![HyperGraphQL](HyperGraphQL.png)  HyperGraphQL
======

HyperGraphQL is a GraphQL query interface over SPARQL endpoints. It enables federated querying of RDF stores using GraphQL query language and schemas mapped at query-time onto the target RDF vocabularies. HyperGraphQL serves two key objectives:

- hiding the complexities of the Semantic Web stack behind the GraphQL server, thus facilitating access to linked data via a simpler and more familiar to many clients GraphQL interface;
- providing a flexible mechanism for restricting access to RDF stores down to naturally defineable subsets of (tree-shaped) queries, which can be efficiently handled by the RDF stores, thus minimising the impact on the stores' availability. 

Alongside the standard JSON-based GraphQL answers, HyperGraphQL delievers also JSON-LD responses conveying full semantic context of the retrieved data. This makes HyperGraphQL a natural query interface for hypermedia-driven Web APIs backed by RDF stores. 

## Schema and context

To configure a HyperGraphQL server one needs to provide only a basic GraphQL type schema and its mapping to the RDF vocabulary of the target SPARQL endpoints. The complete GraphQL wiring is done automatically on starting the server. 

## Execution

To minimise the number of return trips between HyperGraphQL server and RDF stores, the original GraphQL query is translated into the smallest possible number of SPARQL CONSTRUCT queries necessary to fetch all the relevant RDF data. The further transformation of the data into proper GraphQL responses is done locally by the HyperGraphQL server. When the query requests data from a single SPARQL endpoint, only one SPARQL CONSTRUCT query is issued. 

## Running

Clone the Git repository into a local directory. 

**Maven**: in the root directory, first execute **mvn install**, and then **mvn exec:java** (note that in Windows, you need to use *cmd* terminal - not *PowerShell*).

**Gradle**:

## Properties

Basic settings are defined in the *properties.json* file. The defaults are:

```
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
- *graphql.port*: the address of the port at thich the GraphQL server and GraphiQL interface are initiated
- *graphql.path*: the relative URL of the GraphQL server
- *graphql.graphiql*: the relative URL of the GraphiQL UI

## Schema

The schema definition complies with the GraphQL spec (see: 	[http://graphql.org/learn/schema/](http://graphql.org/learn/schema/)). Currently, only the core fragment of the spec, including object types and fields, is supported, as presented in the example below.  


```
type Query {
    person: [Person]
    city: [City]
}

type Person {
    name: [String]
    label: [String]
    birthPlace: City
    birthDate: String
    spouse: [Person]
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

The following example presents a possible context associated with the schema above, where one predicate is associated with the *live.dbpedia.org* endpoint while all the others with *dbpedia.org*.

```
{
  "@predicates": {
    "person": {
      "@id": "http://dbpedia.org/ontology/Person",
      "@namedGraph": "live-dbpedia-endpoint"
    },
    "city": {
      "@id": "http://dbpedia.org/ontology/City",
      "@namedGraph": "dbpedia"
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
    "spouse": {
      "@id": "http://dbpedia.org/property/spouse",
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
    },
    "live-dbpedia": {
      "@id": "",
      "@endpoint": "live-dbpedia-endpoint"
    }
  },
  "@endpoints": {
    "dbpedia-endpoint": {
      "@id": "http://dbpedia.org/sparql/",
      "@user": "",
      "@password": ""
    },
    "live-dbpedia-endpoint": {
      "@id": "http://live.dbpedia.org/sparql",
      "@user": "",
      "@password": ""
    }
  }
}
```
