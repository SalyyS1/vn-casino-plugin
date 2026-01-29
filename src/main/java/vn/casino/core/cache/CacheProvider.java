package vn.casino.core.cache;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Cache provider interface for high-performance data caching.
 * Supports Redis with Caffeine fallback.
 */
public interface CacheProvider {

    /**
     * Initialize the cache connection.
     *
     * @return CompletableFuture that completes when initialization is done
     */
    CompletableFuture<Void> initialize();

    /**
     * Shutdown the cache connection gracefully.
     *
     * @return CompletableFuture that completes when shutdown is done
     */
    CompletableFuture<Void> shutdown();

    /**
     * Set a key-value pair with TTL.
     *
     * @param key Cache key
     * @param value Cache value
     * @param ttl Time to live
     * @return CompletableFuture that completes when operation is done
     */
    CompletableFuture<Void> set(String key, String value, Duration ttl);

    /**
     * Get a value by key.
     *
     * @param key Cache key
     * @return CompletableFuture with optional value
     */
    CompletableFuture<Optional<String>> get(String key);

    /**
     * Delete a key.
     *
     * @param key Cache key
     * @return CompletableFuture that completes when operation is done
     */
    CompletableFuture<Void> delete(String key);

    /**
     * Increment a numeric value.
     *
     * @param key Cache key
     * @return CompletableFuture with new value
     */
    CompletableFuture<Long> increment(String key);

    /**
     * Decrement a numeric value.
     *
     * @param key Cache key
     * @return CompletableFuture with new value
     */
    CompletableFuture<Long> decrement(String key);

    /**
     * Add member to sorted set with score.
     *
     * @param key Sorted set key
     * @param score Member score
     * @param member Member value
     * @return CompletableFuture that completes when operation is done
     */
    CompletableFuture<Void> zadd(String key, double score, String member);

    /**
     * Get members from sorted set in descending order by score.
     *
     * @param key Sorted set key
     * @param start Start index (0-based)
     * @param stop Stop index (-1 for all)
     * @return CompletableFuture with list of members
     */
    CompletableFuture<List<String>> zrevrange(String key, long start, long stop);

    /**
     * Get members with scores from sorted set in descending order.
     *
     * @param key Sorted set key
     * @param start Start index (0-based)
     * @param stop Stop index (-1 for all)
     * @return CompletableFuture with map of member to score
     */
    CompletableFuture<Map<String, Double>> zrevrangeWithScores(String key, long start, long stop);

    /**
     * Set a hash field.
     *
     * @param key Hash key
     * @param field Field name
     * @param value Field value
     * @return CompletableFuture that completes when operation is done
     */
    CompletableFuture<Void> hset(String key, String field, String value);

    /**
     * Get a hash field.
     *
     * @param key Hash key
     * @param field Field name
     * @return CompletableFuture with optional value
     */
    CompletableFuture<Optional<String>> hget(String key, String field);

    /**
     * Get all hash fields and values.
     *
     * @param key Hash key
     * @return CompletableFuture with map of field to value
     */
    CompletableFuture<Map<String, String>> hgetAll(String key);

    /**
     * Delete a hash field.
     *
     * @param key Hash key
     * @param field Field name
     * @return CompletableFuture that completes when operation is done
     */
    CompletableFuture<Void> hdel(String key, String field);

    /**
     * Check if cache is healthy and reachable.
     *
     * @return true if cache is available
     */
    boolean isHealthy();

    /**
     * Get cache type identifier.
     *
     * @return "redis" or "caffeine"
     */
    String getCacheType();
}
