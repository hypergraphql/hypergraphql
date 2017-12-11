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