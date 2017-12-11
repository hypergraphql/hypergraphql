HyperGraphQL is a [GraphQL](http://graphql.org) query interface for RDF triple stores. It enables querying any SPARQL endpoint using GraphQL query language and schemas mapped onto the target RDF vocabularies. 

HyperGraphQL serves two key objectives:

- hiding the complexities of the Semantic Web stack behind the GraphQL server, thus enabling access to linked data via a simpler and more familiar to many clients GraphQL interface;
- providing a flexible mechanism for restricting access to RDF stores down to naturally definable subsets of (tree-shaped) queries, which can be efficiently handled by the RDF stores, thus minimising the impact on the stores' availability. 

The responses of HyperGraphQL are [JSON-LD](http://json-ld.org) objects that convey full semantic context of the fetched data. This makes HyperGraphQL a natural [Linked Data Fragment](http://linkeddatafragments.org) interface for hypermedia-enabled Web APIs, backed by RDF stores. 


