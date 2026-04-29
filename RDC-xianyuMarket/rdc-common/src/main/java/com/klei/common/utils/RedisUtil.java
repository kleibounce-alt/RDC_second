package com.klei.common.utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisUtil {

    private static final JedisPool POOL;

    static {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(20);
        config.setMaxIdle(10);
        config.setMinIdle(5);
        // Docker Redis：本机 6379，无密码
        POOL = new JedisPool(config, "localhost", 6379, 5000);
    }

    public static String get(String key) {
        try (Jedis jedis = POOL.getResource()) {
            return jedis.get(key);
        }
    }

    public static void set(String key, String value) {
        try (Jedis jedis = POOL.getResource()) {
            jedis.set(key, value);
        }
    }

    public static void setex(String key, int seconds, String value) {
        try (Jedis jedis = POOL.getResource()) {
            jedis.setex(key, seconds, value);
        }
    }

    public static void del(String key) {
        try (Jedis jedis = POOL.getResource()) {
            jedis.del(key);
        }
    }

    public static boolean exists(String key) {
        try (Jedis jedis = POOL.getResource()) {
            return jedis.exists(key);
        }
    }

    public static void expire(String key, int seconds) {
        try (Jedis jedis = POOL.getResource()) {
            jedis.expire(key, seconds);
        }
    }

    public static long incr(String key) {
        try (Jedis jedis = POOL.getResource()) {
            return jedis.incr(key);
        }
    }

    public static long ttl(String key) {
        try (Jedis jedis = POOL.getResource()) {
            return jedis.ttl(key);
        }
    }

    public static void close() {
        if (POOL != null && !POOL.isClosed()) {
            POOL.close();
        }
    }
}