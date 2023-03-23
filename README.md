# tugraph-client-java

This repo is a Java client for developers to connect to TuGraph.

## Features

- RPC client in Java
- OGM, short for Object-Graph Mapping, supports mapping entities and relations in graph to Java objects, which speeds up the Java development process.

## Prerequisites

- Java 8
- TuGraph Server deployed

## Usage

### Dependency Integration

If you are using Maven to manage the dependency in your Java project, you can add the following snippet to your pom.xml

```xml
<dependency>
    <groupId>com.antgroup.tugraph</groupId>
    <artifactId>tugraph-db-java-rpc-client</artifactId>
    <version>${version}</version>
</dependency>
<dependency>
    <groupId>com.antgroup.tugraph</groupId>
    <artifactId>tugraph-db-ogm-api</artifactId>
    <version>${version}</version>
</dependency>
<dependency>
    <groupId>com.antgroup.tugraph</groupId>
    <artifactId>tugraph-db-ogm-core</artifactId>
    <version>${version}</version>
</dependency>
<dependency>
    <groupId>com.antgroup.tugraph</groupId>
    <artifactId>tugraph-db-rpc-driver</artifactId>
    <version>${version}</version>
</dependency>
```

### Usage

#### Java client usage

Please refer to the code example:

[Java Client Test](rpc-client-test/src/main/java/com/antgroup/tugraph/TuGraphDbRpcClientTest.java)

#### OGM usage

Please refer to the code example:
[OGM Test](ogm/tugraph-db-ogm-test/src/main/java/test/TestBase.java)

#### OGM API Reference

| Feature                                | API                                                                              |
|----------------------------------------|----------------------------------------------------------------------------------|
| Save object                            | void session.save(T object)                                                      |
| Delete object                          | void session.delete(T object)                                                    |
| Delete with a type                     | void session.deleteAll(Class\<T> type)                                           |
| Purge database                         | void purgeDatabase()                                                             |
| Update an object                       | void session.save(T newObject)                                                   |
| Load with a singe ID                   | T load(Class<T> type, ID id)                                                     |
| Load with IDs                          | Collection\<T> loadAll(Class\<T> type, Collection<ID> ids)                       |
| Load with a type                       | Collection\<T> loadAll(Class\<T> type)                                           |
| Load with filters                      | Collection\<T> loadAll(Class\<T> type, Filters filters)                          |
| Cypher query with specific result type | T queryForObject(Class\<T> objectType, String cypher, Map<String, ?> parameters) |
| Cypher query                           | Result query(String cypher, Map<String, ?> parameters)                           |


## Version Map

| Client Version | TuGraph Version |
|----------------|-----------------|
| 1.1.1          | 3.3.3           |
| 1.2.1          | 3.4.x           |

**Note**:
- 3.3.0~3.3.2 versions of TuGraph Server are legacy versions before java-client refactoring, which are not supported by this repo.
- 1.1.0 and 1.2.0 is not available due to the unconsumable issue introduced by ${revision} variable in pom file[1].

[1] https://stackoverflow.com/questions/41086512/maven-issue-to-build-one-module-using-revision-property
