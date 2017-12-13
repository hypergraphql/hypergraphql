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
    <style>
        .graphiql {
            height: 400px;
        }
</style>
</graphiqlconfig>

This will be our first GraphQL service:

<graphiql>
<div class="graphiql" id="graphiql1">Loading...</div>

<script>
    var parameters = {query: "{ try me 1 }"};
    function onEditQuery(newQuery) {
        parameters.query = newQuery;
    }

    function graphQLFetcher(graphQLParams) {
        return fetch('/graphql1', {
            method: 'post',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(graphQLParams),
            credentials: 'include',
        }).then(function (response) {
            return response.text();
        }).then(function (responseBody) {
            try {
                return JSON.parse(responseBody);
            } catch (error) {
                return responseBody;
            }
        });
    }

    ReactDOM.render(
        React.createElement(GraphiQL, {
            fetcher: graphQLFetcher,
            query: parameters.query,
            onEditQuery: onEditQuery,
        }),
        document.getElementById('graphiql1')
    );
</script>
</graphiql>

This will be our second GraphQL service

<graphiql>
<div class="graphiql" id="graphiql2">Loading...</div>

<script>
    var parameters = {query: "{ try me 2 }"};
    function onEditQuery(newQuery) {
        parameters.query = newQuery;
    }

    function graphQLFetcher(graphQLParams) {
        return fetch('/graphql2', {
            method: 'post',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(graphQLParams),
            credentials: 'include',
        }).then(function (response) {
            return response.text();
        }).then(function (responseBody) {
            try {
                return JSON.parse(responseBody);
            } catch (error) {
                return responseBody;
            }
        });
    }

    ReactDOM.render(
        React.createElement(GraphiQL, {
            fetcher: graphQLFetcher,
            query: parameters.query,
            onEditQuery: onEditQuery,
        }),
        document.getElementById('graphiql2')
    );
</script>
</graphiql>


This will be our third GraphQL service:


<graphiql>
<div class="graphiql" id="graphiql3">Loading...</div>

<script>
    var parameters = {query: "{ try me 3 }"};
    function onEditQuery(newQuery) {
        parameters.query = newQuery;
    }

    function graphQLFetcher(graphQLParams) {
        return fetch('/graphql3', {
            method: 'post',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(graphQLParams),
            credentials: 'include',
        }).then(function (response) {
            return response.text();
        }).then(function (responseBody) {
            try {
                return JSON.parse(responseBody);
            } catch (error) {
                return responseBody;
            }
        });
    }

    ReactDOM.render(
        React.createElement(GraphiQL, {
            fetcher: graphQLFetcher,
            query: parameters.query,
            onEditQuery: onEditQuery,
        }),
        document.getElementById('graphiql3')
    );
</script>
</graphiql>