---
layout: default
title: Demo
permalink: /demo/
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



# Demo

A live demo of the HyperGraphQL instance pointing at DBpedia SPARQL endpoint and configured as in the main GitHub repository (also used as a running example in the [Documentation](/documentation) section) is available at:

- [GraphQL server](/service/graphql4)
- [GraphiQL UI](/service/graphiql4)

Below we list a few example queries via embedded GraphiQL interfaces. Feel free to edit them!

### People and their personal details

<graphiql id="demo1" graphql="graphql4" graphiql="graphiql4" query=
'{
  Person_GET(limit:10, offset:10) {
    _id
    _type
    name
    birthDate
    birthPlace {
      _id
      label(lang:"en")
      country {
        _id
        label(lang:"en")
      }
    }
  }
}'
>
  <script>
       graphiqlInit('demo1');
  </script>
</graphiql>
<br>


### Data about the BMW company

<graphiql id="demo2" graphql="graphql4" graphiql="graphiql4" query=
'{
  Company_GET_BY_ID(uris:[
    "http://dbpedia.org/resource/BMW"
  ]) {
    _id
    _type
    owner {
      _id
      name
    }
    locationCity {
      _id
      label(lang:"en")
      country {
        _id
        label(lang:"en")
      }
    }
  }
}'
>
 <script>
       graphiqlInit('demo2');
</script>
</graphiql>
<br>


### Countries and the leaders of their capital cities

<graphiql id="demo3" graphql="graphql4" graphiql="graphiql4" query=
'{
  Country_GET_BY_ID(uris:[
    "http://dbpedia.org/resource/Italy",
    "http://dbpedia.org/resource/Japan",
    "http://dbpedia.org/resource/Kenya",
    "http://dbpedia.org/resource/Spain",
    "http://dbpedia.org/resource/Canada"
  ]) {
    _id
    _type
    label(lang:"en")
    capital {
    	_id
      label(lang:"en")
      leader {
        name
        birthPlace {
          label(lang:"en")
        }
      }
    }
  }
}'
>
 <script>
       graphiqlInit('demo3');
</script>
</graphiql>
<br>

### Introspection query

Introspection queries allow for accessing the schema of a GraphQL instance using designated GraphQL vocabulary.

<graphiql id="demo4" graphql="graphql4" graphiql="graphiql4" query=
'{
  __schema {
    types {
      name
      description
      fields {
        name
        description
        type {
          name
          kind
          ofType {
            name
            kind
          }
        }
      }
    }
  }
}'
>
 <script>
       graphiqlInit('demo4');
</script>
</graphiql>
<br>
[The internal representation of the schema as an RDF graph in TURTLE format can be also accessed here](/service/graphql4).
