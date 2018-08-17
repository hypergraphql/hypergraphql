---
layout: default
title: Containers and Remote Configuration
permalink: /container/
---

# Overview

This section describes how to run an instance of HyperGraphQL in a container, such as 
[Amazon Web Services ElasticBeanstalk](https://aws.amazon.com/elasticbeanstalk/), as well as how to configure these from 
remote locations,
such as [Amazon Web Services S3](https://aws.amazon.com/s3/).

Although this documentation discusses AWS, the general principles can be applied to any container and storage mechanism.

## Configuration options

The configuration parameters are used to point at the location of configuration files.
These files can reside in a number of places:
- A remote URL
- AmazonS3 (a special case of the above)
- On a filesystem
- On the classpath

### Command Line

Command-line parameters are:

*NB*: parameters can be specified as `-<parameter>` or `--<parameter>`

**`-classpath`** - (Optional) - If present, look for the configuration files on the classpath<br/>
**`-config`** - (Either this or the `--s3` parameter **_must_** be present) - location(s) of the configuration files - 
can be a directory, a file or a list of files, e.g.:<br/>
- `-config /hgql/config`
- `-config /hgql/config/config1.json` 
- `-config /hgql/config/config1.json /hgql/config/config2.json`

**`-s3`** - (Required if `--config` isn't provided) - Location of a configuration file on AWS S3<br/>
**`-u`** - (Required if `--s3` is set) - accessKey<br/>
**`-p`** - (Required if `--s3` is set) - accessSecretKey<br/>
**`-nobanner`** - (Optional) - omit banner on startup  

### Environment Variables

**`hgql_config`** - (Required) - where to look for configuration - S3, URL or filesystem<br/>
**`hgql_username`** - (Optional) - Username / accessKey for remote config resource<br/>
**`hgql_password`** - (Optional) - Password / accessSecretKey for remote config resource<br/>
 
## Running in a container

1. Set up an application to run with Java 8 using the command:<br/>
`java [-Xmx<val>] -jar hypergraphql-<version>-exe.jar [<args>]`<br/>
where the `-X` options can be used to apply various memory options to the JVM see [JVM Memory](http://jvmmemory.com/) 
and [Baeldung](http://www.baeldung.com/jvm-parameters)for a non-comprehensive list<br/>
and the `args` list optionally points to configuration resources
2. If you are not using command-line parameters, for example to reference a URL for configuration, then set environment 
variables as appropriate<br/>
3. Don't forget to upload the executable JAR file
4. Start your container and test it at `http://<container>:<port>/graphiql`

