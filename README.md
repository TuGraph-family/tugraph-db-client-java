# tugraph-client-java

This repo is a Java client for developers to connect to TuGraph.

## Features

- RPC client in Java
- OGM, short for Object-Graph Mapping, supports mapping entities and relations in graph to Java objects, which speeds up the Java development process.

## How to use

### Prerequisites

- Java 8
- TuGraph Server deployed

### Integration

If you are using Maven to manage the dependency in your Java project, you can add the following snippet to your pom.xml

#### Import java rpc client
```xml
<dependency>
    <groupId>com.antgroup.tugraph</groupId>
    <artifactId>tugraph-db-java-rpc-client</artifactId>
    <version>1.2.1</version>
</dependency>
```

#### Import OGM
```
<dependency>
    <groupId>com.antgroup.tugraph</groupId>
    <artifactId>tugraph-db-ogm-api</artifactId>
    <version>1.2.1</version>
</dependency>
<dependency>
    <groupId>com.antgroup.tugraph</groupId>
    <artifactId>tugraph-db-ogm-core</artifactId>
    <version>1.2.1</version>
</dependency>
<dependency>
    <groupId>com.antgroup.tugraph</groupId>
    <artifactId>tugraph-db-rpc-driver</artifactId>
    <version>1.2.1</version>
</dependency>
```

### Java client usage

Please refer to `demo/JavaClientDemo/` in TuGraph-db repo.

### OGM usage

Note: You can refer to the module tugraph-db-ogm-tests for more detail.

#### Define the entity and mapping

```java
// Define Movie entity
@NodeEntity
public class Movie {
    @Id
    private Long id;      // id prop
    private String title; // title prop
    private int released; // released prop

    // Define an edge named ACTS_IN    (actor)-[:ACTS_IN]->(movie)
    @Relationship(type = "ACTS_IN", direction = Relationship.Direction.INCOMING)
    Set<Actor> actors = new HashSet<>();

    public Movie(String title, int year) {
        this.title = title;
        this.released = year;
    }
    public Long getId() {
        return id;
    }
    public void setReleased(int released) {
        this.released = released;
    }
}

// Define Actor entity
@NodeEntity
public class Actor {
    @Id
    private Long id;
    private String name;

    @Relationship(type = "ACTS_IN", direction = Relationship.Direction.OUTGOING)
    private Set<Movie> movies = new HashSet<>();

    public Actor(String name) {
        this.name = name;
    }
    public void actsIn(Movie movie) {
        movies.add(movie);
        movie.getActors().add(this);
    }
}
```

#### Connect to TuGraph

```java
// Config
String databaseUri = "list://ip:port";
String username = "admin";
String password = "password";
// Create driver
Driver driver = new RpcDriver();
Configuration.Builder baseConfigurationBuilder = new Configuration.Builder()
                            .uri(databaseUri).verifyConnection(true).credentials(username, password);
                            driver.configure(baseConfigurationBuilder.build());
driver.configure(baseConfigurationBuilder.build());
// Open session
SessionFactory sessionFactory = new SessionFactory(driver, "entity_path");
Session session = sessionFactory.openSession();
```

#### CRUD with OGM

```java
// Create
Movie jokes = new Movie("Jokes", 1990);
session.save(jokes);

Movie speed = new Movie("Speed", 2019);
Actor alice = new Actor("Alice Neeves");
alice.actsIn(speed);
session.save(speed);

// Delete
session.delete(alice);
Movie m = session.load(Movie.class, jokes.getId());
session.delete(m);

// Update
speed.setReleased(2018);
session.save(speed);

// Read
Collection<Movie> movies = session.loadAll(Movie.class);
Collection<Movie> moviesFilter = session.loadAll(Movie.class, new Filter("released", ComparisonOperator.LESS_THAN, 1995));

// Execute Cypher
HashMap<String, Object> parameters = new HashMap<>();
parameters.put("Speed", 2018);
Movie cm = session.queryForObject(Movie.class, "MATCH (cm:Movie{Speed: $Speed}) RETURN *", parameters);
session.query("CALL db.createVertexLabel('Director', 'name', 'name', STRING, false, 'age', INT16, true)", emptyMap());
session.query("CALL db.createEdgeLabel('DIRECT', '[]')", emptyMap());
Result createResult = session.query(
        "CREATE (n:Movie{title:\"The Shawshank Redemption\", released:1994})" +
        "<-[r:DIRECT]-" +
        "(n2:Director{name:\"Frank Darabont\", age:63})",
        emptyMap());
QueryStatistics statistics = createResult.queryStatistics();
System.out.println("created " + statistics.getNodesCreated() + " vertices");
System.out.println("created " + statistics.getRelationshipsCreated() + " edges");

// Purge database
session.deleteAll(Movie.class);
session.purgeDatabase();
```

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
|----------------| --------------- |
| 1.1.1          |     3.3.x       |
| 1.2.1          |     3.4.x       |

**Note**:
1.1.0、1.2.0 is not available due to the unconsumable issue introduced by ${revision} variable in pom file[1].

[1] https://stackoverflow.com/questions/41086512/maven-issue-to-build-one-module-using-revision-property
