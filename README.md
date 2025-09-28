# Poll App - DAT250 Assignment 4

A fullstack web application combining a **Spring Boot backend** and a **Svelte/Vite frontend**.
The frontend is built and automatically served by Spring Boot.
This project extends **Assignment 4** by adding **Redis integration** for caching and experimenting with Redis datatypes.

---

## Features

* User management with persistence
* Poll creation and voting
* Vote options with proper relationships
* RESTful API endpoints
* Redis integration with **Jedis** client
* Redis use cases: tracking logged-in users and poll vote counts
* Cache for aggregated poll results
* Automated testing (unit + Redis use case tests)
* API documentation with Swagger UI
* Continuous Integration with GitHub Actions
* H2 in-memory database for local development

---

## Persistence Layer

* Entities are persisted using **Jakarta Persistence API (JPA)** with **Hibernate ORM**.
* Relationships between `User`, `Poll`, `VoteOption`, and `Vote` are mapped with `@OneToMany` and `@ManyToOne`.
* IDs use `Long` with auto-generation:

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

* Redis is used to:

    * **Track logged-in users** with the `Set` datatype.
    * **Store poll vote counts** with the `Hash` datatype.
    * **Cache aggregated poll results** (`poll:{id}:votes`) to avoid expensive SQL queries.

---

## API Documentation

Once the application is running, visit:

* Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* API Docs: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

---

## Running the Application

```bash
./gradlew bootRun
```

The backend will start on **[http://localhost:8080](http://localhost:8080)** and serve the frontend automatically.

Make sure **Redis server** is running locally (`redis-server`) before starting the app.

---

## Running Tests

### Unit Tests (Gradle)

```bash
./gradlew test
```

### JPA Persistence Tests

The project includes **integration tests** that verify entity persistence, relationships, and cascade operations using the in-memory H2 database.
Run them with:

```bash
./gradlew test --tests "*PollsTest"
```

All entities (`User`, `Poll`, `VoteOption`, `Vote`) are created, persisted, and queried as part of these tests.

### Redis tests

A dedicated test class `RedisUseCaseTests` replicates Redis CLI commands programmatically using Jedis.
It demonstrates:

* **Use Case 1**: Tracking logged-in users with `SADD`, `SREM`, `SMEMBERS`, and `SISMEMBER`.
* **Use Case 2**: Storing and incrementing poll votes with `HSET`, `HGETALL`, and `HINCRBY`.

Run them directly from IntelliJ or via:

```bash
./gradlew test --tests "*RedisUseCaseTests"
```

---

## Continuous Integration

GitHub Actions (`ci.yml`) automatically:

* Builds the project with Gradle
* Runs all tests
* Ensures code correctness on each push or pull request

