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

A live demo of the HyperGraphQL instance configured as in the main GitHub repository is available at:
- [GraphQL server](/service/graphql4)
- [GraphiQL UI](/service/graphiql4)

You can also try the following predefined queries:

* [people and their personal data](http://hypergraphql.org/graphiql?query=%7B%0A%20%20people(limit%3A50%2C%20offset%3A1000)%20%7B%0A%20%20%20%20_id%0A%20%20%20%20_type%0A%20%20%20%20name%0A%20%20%20%20birthDate%0A%20%20%20%20birthPlace%20%7B%0A%20%20%20%20%20%20_id%0A%20%20%20%20%20%20_type%0A%20%20%20%20%20%20label%20(lang%3A%22en%22)%0A%20%20%20%20%7D%0A%20%20%20%20deathDate%0A%20%20%20%20deathPlace%20%7B%0A%20%20%20%20%20%20_id%0A%20%20%20%20%20%20_type%0A%20%20%20%20%20%20label%20(lang%3A%22en%22)%0A%20%20%20%20%7D%0A%20%20%7D%0A%7D%0A)
* [companies and their locations](http://hypergraphql.org/graphiql?query=%7B%0A%20%20companies(limit%3A%20100)%20%7B%0A%20%20%20%20_id%0A%20%20%20%20_type%0A%20%20%20%20name%0A%20%20%20%20locationCity%20%7B%0A%20%20%20%20%20%20_id%0A%20%20%20%20%20%20label(lang%3A%20%22en%22)%0A%20%20%20%20%20%20country%20%7B%0A%20%20%20%20%20%20%20%20_id%0A%20%20%20%20%20%20%20%20label(lang%3A%20%22en%22)%0A%20%20%20%20%20%20%7D%0A%20%20%20%20%7D%0A%20%20%7D%0A%7D%0A)
* [mountains and first ascents](http://hypergraphql.org/graphiql?query=%7B%0A%20%20mountains(limit%3A%20100%2C%20offset%3A%20100)%20%7B%0A%20%20%20%20_id%0A%20%20%20%20_type%0A%20%20%20%20label%20(lang%3A%22en%22)%0A%20%20%20%20firstAscentPerson%20%7B%0A%20%20%20%20%20%20_id%0A%20%20%20%20%20%20name%0A%20%20%20%20%20%20%0A%20%20%20%20%7D%0A%20%20%7D%0A%7D%0A)

We will try three queries on the same endpoint:

We can also take a look at the internal representation of the RDF schema:
[See the rdf schema](/service/graphql4)

<graphiql id="demo1" graphql="graphql4" graphiql="graphiql4" query=
'{
  Person_GET(limit: 1, offset: 6) {
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
test test


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
      label
      country {
        _id
        label
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

Introspection query

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