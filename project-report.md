# DAT250: Software Technology Experiment Assignment 5 - Hand-in Report

## Project Overview

This assignment extends the poll application by integrating **Redis (Valkey)** as an in-memory data store.
I successfully installed Redis locally, verified its operation through the CLI (`redis-cli`), and explored Redis datatypes such as **Strings**, **Sets**, and **Hashes**.

Three specific use cases were implemented and tested:

1. **Tracking logged-in users** using Redis Sets.
2. **Storing and updating poll vote counts** using Redis Hashes.
3. **Implementing a cache for poll results**: vote counts are now cached in Redis, avoiding repeated aggregation queries against the relational database.

Additionally, I reproduced these same tests in **Java** using the Jedis library, ensuring that the application can interact programmatically with Redis.
The cache implementation was integrated directly into the application’s `PollManager` and exposed via REST endpoints.

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

### 3. **Synchronizing Cache and Database**

**Problem**: After introducing Redis as a cache for poll results, new votes were correctly stored in the database but not reflected in Redis.

**Solution**: Updated the `PollManager` so that every time a vote is registered, the corresponding Redis hash is incremented using `HINCRBY`.
This ensures that Redis and the relational database stay consistent.

---

### 4. **Interpreting Cache vs. Database Results**

**Problem**: At first it was not clear how to verify if the cache was being used instead of the database aggregation query.

**Solution**: We confirmed cache usage by:

* Checking `redis-cli` directly with `HGETALL poll:<id>:votes` to see updated counts.
* Comparing responses from `/polls/{id}/results` before and after caching logic.

If Redis contained the key, the endpoint returned results immediately; if not, the backend fell back to the database query and populated Redis for subsequent requests.

---

## Changes made to the base project

* Added **Jedis dependency** to `build.gradle` for Redis integration.
* Created a new test class `RedisUseCaseTests` under `src/test/java/com/Assigment5/DAT250Assigment5`. This class replicates the CLI Redis operations for Use Case 1 (tracking logged-in users) and Use Case 2 (poll vote counts).
* Extended the `PollManager` with:

    * Methods to update Redis whenever a vote is cast.
    * Methods to fetch poll results from Redis if available, otherwise compute from the database and then store them in Redis.
* Created a new REST controller endpoint `/polls/{id}/results` that retrieves cached results (if present) or falls back to the database.

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

### Implementing Cache for Poll Results

* **Goal**: Avoid running the expensive aggregation query every time a client requests poll results.


* **Explanation**:

    * A Redis Hash stores the vote counts for each option in a poll.
    * When a client requests `/polls/{id}/results`:

        * If Redis contains the hash, the results are returned directly.
        * If Redis does not contain the hash, the system queries the database, aggregates results, returns them, and **stores them in Redis** for faster subsequent access.
    * When a new vote is cast, the corresponding Redis entry is updated immediately with `HINCRBY`, ensuring consistency.


* **How to test using the UI**:

    1. Create a **user** (e.g., Alice).
    2. Create a **poll** with options (e.g., “Pineapple on Pizza?”).
    3. Cast **votes** for one or more options.
    4. Check the results via the UI at `/polls/{pollId}/results`.

        * First request will likely hit the database, but results are also stored in Redis.
    5. Open `redis-cli` and run:

       ```
       HGETALL poll:<pollId>:votes
       ```

       You should see each `presentationOrder` and its vote count.
    6. Cast additional votes and confirm that:

        * The numbers in Redis update automatically.
        * The results endpoint responds instantly without hitting the database.


* **Before vs After Cache Comparison**:

    * **Before**: Every call to `/polls/{id}/results` triggered the SQL query:

      ```sql
      SELECT o.presentationOrder, COUNT(v.id)
      FROM vote_options o
      INNER JOIN votes v on o.id = v.voted_on
      WHERE o.poll = ?
      GROUP BY o.presentationOrder
      ORDER BY o.presentationOrder;
      ```

      This aggregation ran even if the poll had already been fetched moments earlier.

    * **After**: The same endpoint now first checks Redis:

        * If cached → returns results immediately in O(1).
        * If not cached → runs the SQL query once, caches the result, and reuses it.

  This drastically reduces database load when results are requested frequently.


---

## Test Scenario

The application supports all previous scenarios (user creation, poll creation, voting, persistence via JPA/H2, etc.).

In addition, for Assignment 5:

1. **Redis CLI Tests**: Verified Redis works locally with commands `PING`, `SET`, `GET`, `EXPIRE`, `SADD`, `SREM`, `SMEMBERS`, `HSET`, `HGETALL`, and `HINCRBY`.
2. **Java Jedis Tests**: Verified programmatic access to Redis with Use Case 1 (tracking logged-in users) and Use Case 2 (poll vote counts).
3. **Cache Integration Tests**: Verified that:

    * `/polls/{id}/results` first checks Redis.
    * New votes update both the database and Redis immediately.
    * Results are faster on subsequent requests thanks to caching.


---

## Link to Code

* Code from Assigment 1: [https://github.com/WonderBattle/DAT250-Assigment1](https://github.com/WonderBattle/DAT250-Assigment1)
* Code from Assigment 2: [https://github.com/WonderBattle/DAT250-Assigment2](https://github.com/WonderBattle/DAT250-Assigment2)
* Code from Assigment 3: [https://github.com/WonderBattle/DAT250-Assigment3](https://github.com/WonderBattle/DAT250-Assigment3)
* Code from Assigment 4: [https://github.com/WonderBattle/DAT250-Assigment4](https://github.com/WonderBattle/DAT250-Assigment4)
* Code from Assigment 5: [https://github.com/WonderBattle/DAT250-Assigment5](https://github.com/WonderBattle/DAT250-Assigment5)


---

## Key Features Implemented

### ✅ Redis Integration

* Jedis dependency added to Gradle.
* Redis connection pool established for Java usage.

### ✅ Redis Use Cases

* **Set datatype** used to track logged-in users.
* **Hash datatype** used to store poll vote counts.

### ✅ Cache Implementation

* `PollManager` extended to:

    * Store poll results in Redis Hashes.
    * Fetch results from cache when available.
    * Invalidate/update Redis counts when new votes are cast.
* New endpoint `/polls/{id}/results` returns cached results when possible.

### ✅ Verification

* Results tested both via **UI** and **redis-cli**.
* Confirmed consistency between relational DB and Redis data.

---

## Pending Issues

### 1. **Time-to-Live (TTL)**

Currently cached poll results in Redis never expire. A TTL policy could be added to avoid stale entries for long-closed polls.

### 2. **Cache Invalidation for Poll Deletion**

If a poll is deleted from the database, its Redis entry remains. A cleanup mechanism is needed.

### 3. **Scalability Considerations**

The cache implementation works for a single Redis instance. For larger deployments, clustering or replication should be considered.

### 4. **Spring Data Redis Integration**

Instead of using Jedis directly, the project could adopt **Spring Data Redis**. This would simplify cache handling by using Spring’s `RedisTemplate` or built-in cache abstraction with annotations like `@Cacheable`.

### 5. **Redis Cluster and Sharding**

Setting up a **Redis cluster with multiple nodes** would distribute data across shards. This improves fault tolerance and scalability, but requires deciding on a sharding strategy and verifying data distribution.

### 6. **Redis as Sole Database**

A possible extension is replacing JPA entirely by **serializing domain objects into Redis Hashes or JSON**. This would mean Redis is not only the cache but also the primary database. It requires revisiting persistence logic and ensuring durability.

---

## Conclusion

The assignment was successfully completed with Redis fully integrated into the poll application.
I implemented three distinct Redis use cases and, most importantly, extended the application with a **poll results cache**, drastically reducing the number of expensive aggregation queries.

The main challenges involved synchronizing Redis with database changes and confirming cache hits vs. database queries. By resolving these, I achieved a consistent, performant solution.
This assignment provided valuable experience in caching strategies, database–cache consistency, and practical Redis usage in a real web application.

