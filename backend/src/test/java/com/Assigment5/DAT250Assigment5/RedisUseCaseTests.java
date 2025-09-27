package com.Assigment5.DAT250Assigment5;

import redis.clients.jedis.JedisPooled;

import java.util.Map;
import java.util.Set;

public class RedisUseCaseTests {

    public static void main(String[] args) {
        try (JedisPooled jedis = new JedisPooled("localhost", 6379)) {
            System.out.println("=== USE CASE 1: Logged-in users with SET ===");
            useCase1(jedis);

            System.out.println("\n=== USE CASE 2: Poll votes with HASH ===");
            useCase2(jedis);
        }
    }

    private static void useCase1(JedisPooled jedis) {
        String key = "loggedin";
        jedis.del(key); // clean slate

        // Initial state
        Set<String> members = jedis.smembers(key);
        System.out.println("Initial state: " + members);

        // User "alice" logs in
        jedis.sadd(key, "alice");
        System.out.println("After alice logs in: " + jedis.smembers(key));

        // User "bob" logs in
        jedis.sadd(key, "bob");
        System.out.println("After bob logs in: " + jedis.smembers(key));

        // User "alice" logs off
        jedis.srem(key, "alice");
        System.out.println("After alice logs off: " + jedis.smembers(key));

        // User "eve" logs in
        jedis.sadd(key, "eve");
        System.out.println("After eve logs in: " + jedis.smembers(key));

        // Membership checks
        System.out.println("Is bob logged in? " + jedis.sismember(key, "bob"));
        System.out.println("Is alice logged in? " + jedis.sismember(key, "alice"));
    }

    private static void useCase2(JedisPooled jedis) {
        String pollId = "03ebcb7b";
        String metaKey = "poll:" + pollId + ":meta";
        String captionsKey = "poll:" + pollId + ":captions";
        String countsKey = "poll:" + pollId + ":counts";

        // Clean slate
        jedis.del(metaKey, captionsKey, countsKey);

        // Store metadata
        jedis.hset(metaKey, "id", pollId);
        jedis.hset(metaKey, "title", "Pineapple on Pizza?");
        System.out.println("Meta: " + jedis.hgetAll(metaKey));

        // Store captions
        jedis.hset(captionsKey, "0", "Yes, yammy!");
        jedis.hset(captionsKey, "1", "Mamma mia, nooooo!");
        jedis.hset(captionsKey, "2", "I do not really care ...");
        System.out.println("Captions: " + jedis.hgetAll(captionsKey));

        // Store initial counts
        jedis.hset(countsKey, "0", "269");
        jedis.hset(countsKey, "1", "268");
        jedis.hset(countsKey, "2", "42");
        System.out.println("Initial counts: " + jedis.hgetAll(countsKey));

        // Increment option 0 (Yes, yammy!)
        long newCount = jedis.hincrBy(countsKey, "0", 1);
        System.out.println("After one new 'Yes' vote → option 0 count = " + newCount);

        // Show all counts again
        Map<String, String> counts = jedis.hgetAll(countsKey);
        System.out.println("Final counts: " + counts);
    }
}

