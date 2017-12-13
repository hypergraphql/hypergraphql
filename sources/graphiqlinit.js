function getEditFunction(parameters) {
    return function onEditQuery(newQuery) {
        parameters.query = newQuery;
    }
};

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
};

function graphiqlInit(name) {
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
    full.setAttribute('href', '/service/' + graphiql + '?query=' + encodeURI(queryString));
    alert(queryString);
    alert(encodeURI(queryString));
    gqlelement.appendChild(full);
    var parameters = {query: queryString};
    ReactDOM.render(
        React.createElement(GraphiQL, {
            fetcher: getFetchingFunction('/service/' + graphql),
            query: parameters.query,
            onEditQuery: getEditFunction(parameters),
        }),
        document.getElementById(name + '_dashboard')
    );
};
