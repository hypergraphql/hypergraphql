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
    <script> 
        function getEditFunction(parameters) {
            return function onEditQuery(newQuery) {
                parameters.query = newQuery;
            }
        }
        function getFetchingFunction(url) {
            return function graphQLFetcher(graphQLParams) {
                return fetch(url, {
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
        }
    </script>
</graphiqlconfig>

This will be our first GraphQL service:

<graphiql id="tutorial1" graphql="graphql1" graphiql="graphiql1" query="{ try me 1 }">
    <script>
        var name = 'tutorial1';
        var gqlelement = document.getElementById(name);
        var graphql = gqlelement.attributes['graphql'].value;
        var graphiql = gqlelement.attributes['graphiql'].value;
        var queryString = gqlelement.attributes['query'].value;
        var div = document.createElement('div');
        div.textContent = "Loading...";
        div.setAttribute('class', 'graphiql');
        div.setAttribute('id', name + '_dashboard');
        gqlelement.appendChild(div);
        var full = document.createElement('a');
        full.textContent = "See in fullscreen mode.";
        full.setAttribute('href', '/hypergraphql/service/' + graphiql + '?query=' + queryString);
        gqlelement.appendChild(full);
        var parameters = {query: queryString};
        ReactDOM.render(
            React.createElement(GraphiQL, {
                fetcher: getFetchingFunction('/hypergraphql/service/' + graphql),
                query: parameters.query,
                onEditQuery: getEditFunction(parameters),
            }),
            document.getElementById(name + '_dashboard')
        );
    </script>
</graphiql>

This will be our second GraphQL service

<graphiql>
<div class="graphiql" id="tutorial2">Loading...</div>

<script>
    var parameters = {query: "{ try me 2 }"};
    function onEditQuery(newQuery) {
        parameters.query = newQuery;
    }

    function graphQLFetcher(graphQLParams) {
        return fetch('/hypergraphql/service/graphql2', {
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
        document.getElementById('tutorial2')
    );
</script>
</graphiql>
[See in fullscreen mode](/hypergraphql/service/graphiql2?query={ service 2 }). 

This will be our third GraphQL service:


<graphiql>
<div class="graphiql" id="tutorial3">Loading...</div>

<script>
    var parameters = {query: "{ try me 3 }"};
    function onEditQuery(newQuery) {
        parameters.query = newQuery;
    }

    function graphQLFetcher(graphQLParams) {
        return fetch('/hypergraphql/service/graphql3', {
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
        document.getElementById('tutorial3')
    );
</script>
</graphiql>
[See in fullscreen mode](/hypergraphql/service/graphiql3?query={ service 3 }). 