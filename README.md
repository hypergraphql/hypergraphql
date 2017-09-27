# graphql-sparql-java
This is a working repository for a GraphQL server over a SPARQL endpoint, implemented in Java.

The GraphQL server starts by default at http://localhost:8009 and the GraphiQL IDE at http://localhost:8009/graphql. For testing purposes is currently pointing to the following SPARQL endpoint:
http://dbpedia.org/sparql

(see properties.json file)

A valid JSON-LD object is attached in the "extensions" section of the GraphQL response, as the value of the "json-ld" property.

