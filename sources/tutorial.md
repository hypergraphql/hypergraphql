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
    <script type="application/javascript" src="https://semantic-integration.github.io/hypergraphql/sources/graphiqlinit.js"></script>
</graphiqlconfig>


This will be our first GraphQL service:



<graphiql id="tutorial1" graphql="graphql1" graphiql="graphiql1" query=
"{
  Person_GET(limit:10) {
    _id
    name
  }
}"
>
    <script>
       graphiqlInit('tutorial1');
    </script>
</graphiql>



<br>
This will be our second GraphQL service



<graphiql id="tutorial2" graphql="graphql2" graphiql="graphiql2" query=
"{
  Person_GET(limit:10) {
    _id
    name
  }
}"
>
    <script>
       graphiqlInit('tutorial2');
    </script>
</graphiql>


<br>
This will be our third GraphQL service:



<graphiql id="tutorial3" graphql="graphql3" graphiql="graphiql3" query=
"{
  Person_GET(limit:10) {
    _id
    name
  }
}"
>
    <script>
       graphiqlInit('tutorial3');
    </script>
</graphiql>
<br>