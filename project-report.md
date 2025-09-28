# DAT250: Software Technology Experiment Assignment 5 - Hand-in Report

## Project Overview

This assignment extends the poll application by integrating **Redis (Valkey)** as an in-memory data store.
We successfully installed Redis locally, verified its operation through the CLI (`redis-cli`), and explored Redis datatypes such as **Strings**, **Sets**, and **Hashes**.

Two specific use cases were implemented and tested:

1. **Tracking logged-in users** using Redis Sets.
2. **Storing and updating poll vote counts** using Redis Hashes.

Additionally, we reproduced these same tests in **Java** using the Jedis library, ensuring that the application can interact programmatically with Redis.

---

## Technical Problems Encountered and Solutions

### 1. **Missing Jedis Dependency**

**Problem**: The code could not resolve `redis.clients.jedis` imports when implementing the Java tests.

**Solution**: Added Jedis as a dependency in `build.gradle`:

```groovy
implementation 'redis.clients:jedis:6.2.0'
```

Then refreshed Gradle to fetch the dependency.

---

### 2. **Verifying Redis Server Availability**

**Problem**: Tests failed when Redis was not running.

**Solution**: Ensured Redis server was started and accessible at `localhost:6379` before running CLI or Java tests. Verified using:

```bash
redis-cli PING
```
Expected response: `PONG`.

---

## Changes made to the base project

* Added **Jedis dependency** to `build.gradle` for Redis integration.
* Created a new test class `RedisUseCaseTests` under `src/test/java/no/ntnu/dat250/expass5/`.
* This class replicates the CLI Redis operations for Use Case 1 (tracking logged-in users) and Use Case 2 (poll vote counts).

No changes were required in the existing poll application source code yet, since this step focuses only on testing Redis basics.

---

## Redis Tests and Use Cases

### Use Case 1: Keep Track of Logged-In Users (Set datatype)

* **Goal**: Use Redis Sets to maintain a collection of currently logged-in users.

* **Explanation**:

    * A `Set` is ideal because it prevents duplicates and allows efficient add/remove operations.
    * We can add users when they log in (`SADD`), remove them when they log off (`SREM`), list all logged-in users (`SMEMBERS`), and check if a user is currently logged in (`SISMEMBER`).

* **Commands and Results**:

```
127.0.0.1:6379> SADD loggedin alice
(integer) 1

127.0.0.1:6379> SADD loggedin bob
(integer) 1

127.0.0.1:6379> SMEMBERS loggedin
1) "alice"
2) "bob"

127.0.0.1:6379> SREM loggedin alice
(integer) 1

127.0.0.1:6379> SMEMBERS loggedin
1) "bob"

127.0.0.1:6379> SADD loggedin eve
(integer) 1

127.0.0.1:6379> SMEMBERS loggedin
1) "bob"
2) "eve"

127.0.0.1:6379> SISMEMBER loggedin bob
(integer) 1

127.0.0.1:6379> SISMEMBER loggedin alice
(integer) 0
```

* **Expected Behavior**:

    * `alice` and `bob` initially log in.
    * `alice` logs off, leaving only `bob`.
    * `eve` logs in, resulting in `bob` and `eve` in the set.
    * Membership checks confirm `bob` is logged in, but `alice` is not.

---

### Use Case 2: Store Poll Votes (Hash datatype)

* **Goal**: Represent a poll and its vote counts using a Redis Hash.

* **Explanation**:

    * Hashes are key–value maps inside Redis, making them suitable for storing structured data.
    * Each option’s caption and vote count can be stored under separate fields.
    * Vote counts can be incremented atomically with `HINCRBY`, avoiding the need to rewrite the entire object.

* **Commands and Results**:

```
127.0.0.1:6379> HSET poll:03ebcb7b:id id "03ebcb7b"
(integer) 1

127.0.0.1:6379> HSET poll:03ebcb7b:title title "Pineapple on Pizza?"
(integer) 1

127.0.0.1:6379> HSET poll:03ebcb7b:options 0 "Yes, yammy!" 1 "Mamma mia, nooooo!" 2 "I do not really care ..."
(integer) 3

127.0.0.1:6379> HSET poll:03ebcb7b:counts 0 269 1 268 2 42
(integer) 3

127.0.0.1:6379> HINCRBY poll:03ebcb7b:counts 0 1
(integer) 270

127.0.0.1:6379> HGETALL poll:03ebcb7b:counts
1) "0"
2) "270"
3) "1"
4) "268"
5) "2"
6) "42"
```

* **Expected Behavior**:

    * Poll metadata and options are stored in hash structures.
    * Initial vote counts are set.
    * Incrementing the vote count for option `0` increases its total from `269` to `270`.
    * The final hash shows updated counts for all options.

---

### Java Implementation

A Java class `RedisUseCaseTests` was created under `src/test/java/com/Assigment5/DAT250Assigment5`.
This class reproduces the CLI operations programmatically using **Jedis** (`JedisPooled` client).

* **Structure**:

    * `useCase1()`: Implements the Set operations for logged-in users.
    * `useCase2()`: Implements the Hash operations for poll vote counts.
    * `main()`: Calls both use cases sequentially.

* **Execution**:

    * Right-click on `RedisUseCaseTests` → **Run 'RedisUseCaseTests.main()'** in IntelliJ.
    * Expected output matches the CLI results, e.g.:

```
=== USE CASE 1: Logged-in users with SET ===
Initial state: []
After alice logs in: [alice]
After bob logs in: [bob, alice]
After alice logs off: [bob]
After eve logs in: [bob, eve]
Is bob logged in? true
Is alice logged in? false

=== USE CASE 2: Poll votes with HASH ===
Meta: {title=Pineapple on Pizza?, id=03ebcb7b}
Captions: {0=Yes, yammy!, 1=Mamma mia, nooooo!, 2=I do not really care ...}
Initial counts: {0=269, 1=268, 2=42}
After one new 'Yes' vote → option 0 count = 270
Final counts: {0=270, 1=268, 2=42}
```



## Test Scenario

The application supports all previous scenarios (user creation, poll creation, voting, persistence via JPA/H2, etc.).

In addition, for Assignment 5:

1. **Redis CLI Tests**: Verified Redis works locally with commands `PING`, `SET`, `GET`, `EXPIRE`, `SADD`, `SREM`, `SMEMBERS`, `HSET`, `HGETALL`, and `HINCRBY`.
2. **Java Jedis Tests**: Verified programmatic access to Redis with Use Case 1 (tracking logged-in users) and Use Case 2 (poll vote counts).

---

## Link to Code

* Code from Assigment 1: [https://github.com/WonderBattle/DAT250-Assigment1](https://github.com/WonderBattle/DAT250-Assigment1)
* Code from Assigment 2: [https://github.com/WonderBattle/DAT250-Assigment2](https://github.com/WonderBattle/DAT250-Assigment2)
* Code from Assigment 3: [https://github.com/WonderBattle/DAT250-Assigment3](https://github.com/WonderBattle/DAT250-Assigment3)
* Code from Assigment 4: [https://github.com/WonderBattle/DAT250-Assigment4](https://github.com/WonderBattle/DAT250-Assigment4)
* Code from Assigment 5: [https://github.com/WonderBattle/DAT250-Assigment5](https://github.com/WonderBattle/DAT250-Assigment5)


---

## Key Features Implemented

### ✅ Entity Annotations

* `@Entity` added to `Poll`, `VoteOption`, `Vote`, and `User`.
* `@Id` and `@GeneratedValue` for primary keys.

### ✅ Relationships

* **Poll ↔ Vote**: One-to-many, mapped by `poll`.
* **Poll ↔ VoteOption**: One-to-many, mapped by `poll`.
* **VoteOption ↔ Vote**: One-to-many, mapped by `votesOn`.
* **Vote ↔ VoteOption**: Many-to-one with join column `option_id`.
* **Vote ↔ Poll**: Many-to-one with implicit join column `poll_id`.

### ✅ Database Schema

Hibernate automatically generated the following tables in H2:

* `poll`
* `vote_option`
* `vote`
* `user`

Foreign keys:

* `vote.poll_id → poll.id`
* `vote.option_id → vote_option.id`
* `vote_option.poll_id → poll.id`

### ✅ Frontend Integration

* Adapted React code to properly send and handle numeric (`Long`) IDs.
* Fixed duplicated vote option issue by cleaning up relationship handling in backend.

---

## Pending Issues

### 1. **Column Naming**

Currently relying on Hibernate’s default column names. For production, explicit `@JoinColumn(name = "...")` annotations should be standardized.

### 2. **Validation**

No validation annotations (`@NotNull`, `@Size`) implemented yet for entity fields.

### 3. **Cascade Deletes**

Although cascading works for some relationships, deletion scenarios (e.g., removing a `Poll`) need more thorough testing.

### 4. **PollManager**

Still uses in-memory `HashMap` storage for certain operations. Full migration to JPA repositories would simplify persistence handling.

### 5. **Warnings in Tests**

Some warnings appear when running GitHub Actions tests (mainly from old `requests.http` UUID IDs), but all tests pass correctly. These should be aligned with `Long` IDs for consistency.

---

## Conclusion

The assignment was successfully completed with a fully working persistence layer for polls, vote options, and votes.
The main challenges involved adapting entity IDs from `String` to `Long`, fixing missing relationships, solving poll ownership issues, avoiding duplicate vote options, and ensuring frontend–backend consistency.

By resolving these, Hibernate correctly generated the database schema, and the provided test case passed.
This assignment provided valuable experience with JPA, Hibernate mappings, schema inspection in H2, and debugging real-world integration issues across backend and frontend.

