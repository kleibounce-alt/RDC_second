package com.klei.common.utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.params.SetParams;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class RedisUtil {

    private static final JedisPool POOL;

    static {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(20);
        config.setMaxIdle(10);
        config.setMinIdle(5);
        POOL = new JedisPool(config, "localhost", 6379, 5000);
    }

    // ========== String ==========
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

    // 分布式锁核心：set if not exists
    public static boolean setnx(String key, String value) {
        try (Jedis jedis = POOL.getResource()) {
            return jedis.setnx(key, value) == 1;
        }
    }


    // 带过期时间的 setnx（原子操作）
    public static boolean setnxex(String key, String value, int seconds) {
        try (Jedis jedis = POOL.getResource()) {
            String result = jedis.set(key, value, SetParams.setParams().nx().ex(seconds));
            return "OK".equals(result);
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

    public static long ttl(String key) {
        try (Jedis jedis = POOL.getResource()) {
            return jedis.ttl(key);
        }
    }

    public static long incr(String key) {
        try (Jedis jedis = POOL.getResource()) {
            return jedis.incr(key);
        }
    }

    public static long incrBy(String key, long increment) {
        try (Jedis jedis = POOL.getResource()) {
            return jedis.incrBy(key, increment);
        }
    }

    // ========== List（队列） ==========
    public static long lpush(String key, String... values) {
        try (Jedis jedis = POOL.getResource()) {
            return jedis.lpush(key, values);
        }
    }

    public static String rpop(String key) {
        try (Jedis jedis = POOL.getResource()) {
            return jedis.rpop(key);
        }
    }

    public static String lpop(String key) {
        try (Jedis jedis = POOL.getResource()) {
            return jedis.lpop(key);
        }
    }

    public static long llen(String key) {
        try (Jedis jedis = POOL.getResource()) {
            return jedis.llen(key);
        }
    }

    public static List<String> lrange(String key, long start, long end) {
        try (Jedis jedis = POOL.getResource()) {
            return jedis.lrange(key, start, end);
        }
    }

    // ========== Set ==========
    public static long sadd(String key, String... members) {
        try (Jedis jedis = POOL.getResource()) {
            return jedis.sadd(key, members);
        }
    }

    public static long srem(String key, String... members) {
        try (Jedis jedis = POOL.getResource()) {
            return jedis.srem(key, members);
        }
    }

    public static boolean sismember(String key, String member) {
        try (Jedis jedis = POOL.getResource()) {
            return jedis.sismember(key, member);
        }
    }

    public static Set<String> smembers(String key) {
        try (Jedis jedis = POOL.getResource()) {
            return jedis.smembers(key);
        }
    }

    // ========== Hash ==========
    public static void hset(String key, String field, String value) {
        try (Jedis jedis = POOL.getResource()) {
            jedis.hset(key, field, value);
        }
    }

    public static String hget(String key, String field) {
        try (Jedis jedis = POOL.getResource()) {
            return jedis.hget(key, field);
        }
    }

    public static Map<String, String> hgetAll(String key) {
        try (Jedis jedis = POOL.getResource()) {
            return jedis.hgetAll(key);
        }
    }

    public static long hdel(String key, String... fields) {
        try (Jedis jedis = POOL.getResource()) {
            return jedis.hdel(key, fields);
        }
    }

    // ========== 批量 & 模糊 ==========
    public static List<String> mget(String... keys) {
        try (Jedis jedis = POOL.getResource()) {
            return jedis.mget(keys);
        }
    }

    public static void mset(String... keyvalues) {
        try (Jedis jedis = POOL.getResource()) {
            jedis.mset(keyvalues);
        }
    }

    // 按前缀查 keys
    public static Set<String> keys(String pattern) {
        try (Jedis jedis = POOL.getResource()) {
            return jedis.keys(pattern);
        }
    }

    // ========== 资源关闭 ==========
    public static void close() {
        if (POOL != null && !POOL.isClosed()) {
            POOL.close();
        }
    }
}