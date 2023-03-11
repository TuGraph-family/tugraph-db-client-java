# tugraph-client-java

This repo is a Java client for developers to connect to TuGraph.

## Features

- RPC client in Java
- OGM(Object-Graph Mapping)(**OnGoing**)

## How to use

### Prerequisites

- Java 8
- TuGraph Server deployed

### Integration

If you are using Maven to manage the dependency in your Java project, you can add the following snippet to your pom.xml
```xml
<dependency>
    <groupId>com.antgroup.tugraph</groupId>
    <artifactId>tugraph-java-rpc-client</artifactId>
    <version>1.1.0</version>
</dependency>
```

### Code example

Please refer to `demo/JavaClientDemo/` in TuGraph repo.

## Version Map

| Client Version | TuGraph Version |
| -------------- | --------------- |
|     1.1.0      |     3.3.x       |