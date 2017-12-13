---
layout: default
title: Demo
permalink: /demo/
---

A live demo of the HyperGraphQL server, configured as in this repository, is available at: [http://hypergraphql.org/graphiql](http://hypergraphql.org/graphiql)

You can also try the following predefined queries:

* [people and their personal data](http://hypergraphql.org/graphiql?query=%7B%0A%20%20people(limit%3A50%2C%20offset%3A1000)%20%7B%0A%20%20%20%20_id%0A%20%20%20%20_type%0A%20%20%20%20name%0A%20%20%20%20birthDate%0A%20%20%20%20birthPlace%20%7B%0A%20%20%20%20%20%20_id%0A%20%20%20%20%20%20_type%0A%20%20%20%20%20%20label%20(lang%3A%22en%22)%0A%20%20%20%20%7D%0A%20%20%20%20deathDate%0A%20%20%20%20deathPlace%20%7B%0A%20%20%20%20%20%20_id%0A%20%20%20%20%20%20_type%0A%20%20%20%20%20%20label%20(lang%3A%22en%22)%0A%20%20%20%20%7D%0A%20%20%7D%0A%7D%0A)
* [companies and their locations](http://hypergraphql.org/graphiql?query=%7B%0A%20%20companies(limit%3A%20100)%20%7B%0A%20%20%20%20_id%0A%20%20%20%20_type%0A%20%20%20%20name%0A%20%20%20%20locationCity%20%7B%0A%20%20%20%20%20%20_id%0A%20%20%20%20%20%20label(lang%3A%20%22en%22)%0A%20%20%20%20%20%20country%20%7B%0A%20%20%20%20%20%20%20%20_id%0A%20%20%20%20%20%20%20%20label(lang%3A%20%22en%22)%0A%20%20%20%20%20%20%7D%0A%20%20%20%20%7D%0A%20%20%7D%0A%7D%0A)
* [mountains and first ascents](http://hypergraphql.org/graphiql?query=%7B%0A%20%20mountains(limit%3A%20100%2C%20offset%3A%20100)%20%7B%0A%20%20%20%20_id%0A%20%20%20%20_type%0A%20%20%20%20label%20(lang%3A%22en%22)%0A%20%20%20%20firstAscentPerson%20%7B%0A%20%20%20%20%20%20_id%0A%20%20%20%20%20%20name%0A%20%20%20%20%20%20%0A%20%20%20%20%7D%0A%20%20%7D%0A%7D%0A)

<graphiqlconfig>
    <script src="//cdn.jsdelivr.net/es6-promise/4.0.5/es6-promise.auto.min.js"></script>
    <script src="//cdn.jsdelivr.net/fetch/0.9.0/fetch.min.js"></script>
    <script src="//cdn.jsdelivr.net/react/15.4.2/react.min.js"></script>
    <script src="//cdn.jsdelivr.net/react/15.4.2/react-dom.min.js"></script>
    <link rel="stylesheet" href="//cdn.jsdelivr.net/npm/graphiql@0.11.2/graphiql.css" />
    <script src="//cdn.jsdelivr.net/npm/graphiql@0.11.2/graphiql.js"></script>
    <script type="application/javascript" src="https://semantic-integration.github.io/hypergraphql/sources/graphiqlinit.js"></script>
</graphiqlconfig>

We will try three queries on the same endpoint:

We can also take a look at the internal representation of the RDF schema:
[See the rdf schema](/hypergraphql/service/graphql4)


<br>
<graphiql id="demo1" graphql="graphql4" graphiql="graphiql4" query=
"{
  Person_GET(limit:10) {
    _id
    name
  }
}"
>
    <script>
       graphiqlInit('demo1');
    </script>
</graphiql>
<br>

test test

<br>
<graphiql id="demo2" graphql="graphql4" graphiql="graphiql4" query=
"{
  Person_GET(limit:10) {
    _id
    name
  }
}"
>
 <script>
       graphiqlInit('demo2');
</script>

</graphiql>
<br>
