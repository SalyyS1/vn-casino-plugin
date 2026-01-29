package vn.casino.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.resps.Tuple;
import vn.casino.core.cache.RedisCacheProvider;
import vn.casino.core.config.MainConfig;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisCacheProvider.
 * Tests Redis operations: set/get/zadd/hset and connection management.
 */
@DisplayName("RedisCacheProvider Tests")
class RedisCacheProviderTest {

    @Mock
    private MainConfig config;

    @Mock
    private Logger logger;

    @Mock
    private JedisPool jedisPool;

    @Mock
    private Jedis jedis;

    private RedisCacheProvider cacheProvider;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock config values
        when(config.getRedisHost()).thenReturn("localhost");
        when(config.getRedisPort()).thenReturn(6379);
        when(config.getRedisTimeout()).thenReturn(2000);
        when(config.getRedisDatabase()).thenReturn(0);
        when(config.getRedisPassword()).thenReturn(null);
        when(config.getRedisPoolSize()).thenReturn(8);

        cacheProvider = new RedisCacheProvider(config, logger);
    }

    @Test
    @DisplayName("Set should store value with TTL")
    void testSet() throws Exception {
        String key = "test:key";
        String value = "test-value";
        Duration ttl = Duration.ofMinutes(5);

        when(jedisPool.getResource()).thenReturn(jedis);
        doNothing().when(jedis).setex(eq(key), eq(ttl.toSeconds()), eq(value));

        // Use reflection to inject mock JedisPool
        injectMockJedisPool();

        CompletableFuture<Void> future = cacheProvider.set(key, value, ttl);
        future.get();

        verify(jedis).setex(key, ttl.toSeconds(), value);
        verify(jedis).close();
    }

    @Test
    @DisplayName("Get should retrieve value")
    void testGet() throws Exception {
        String key = "test:key";
        String expectedValue = "test-value";

        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.get(key)).thenReturn(expectedValue);

        injectMockJedisPool();

        CompletableFuture<Optional<String>> future = cacheProvider.get(key);
        Optional<String> result = future.get();

        assertTrue(result.isPresent());
        assertEquals(expectedValue, result.get());
        verify(jedis).get(key);
        verify(jedis).close();
    }

    @Test
    @DisplayName("Get should return empty on missing key")
    void testGetMissingKey() throws Exception {
        String key = "missing:key";

        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.get(key)).thenReturn(null);

        injectMockJedisPool();

        CompletableFuture<Optional<String>> future = cacheProvider.get(key);
        Optional<String> result = future.get();

        assertFalse(result.isPresent());
        verify(jedis).get(key);
    }

    @Test
    @DisplayName("Delete should remove key")
    void testDelete() throws Exception {
        String key = "test:key";

        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.del(key)).thenReturn(1L);

        injectMockJedisPool();

        CompletableFuture<Void> future = cacheProvider.delete(key);
        future.get();

        verify(jedis).del(key);
        verify(jedis).close();
    }

    @Test
    @DisplayName("Increment should increase counter")
    void testIncrement() throws Exception {
        String key = "counter:key";
        Long expectedValue = 42L;

        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.incr(key)).thenReturn(expectedValue);

        injectMockJedisPool();

        CompletableFuture<Long> future = cacheProvider.increment(key);
        Long result = future.get();

        assertEquals(expectedValue, result);
        verify(jedis).incr(key);
        verify(jedis).close();
    }

    @Test
    @DisplayName("Decrement should decrease counter")
    void testDecrement() throws Exception {
        String key = "counter:key";
        Long expectedValue = 10L;

        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.decr(key)).thenReturn(expectedValue);

        injectMockJedisPool();

        CompletableFuture<Long> future = cacheProvider.decrement(key);
        Long result = future.get();

        assertEquals(expectedValue, result);
        verify(jedis).decr(key);
        verify(jedis).close();
    }

    @Test
    @DisplayName("Zadd should add member to sorted set")
    void testZadd() throws Exception {
        String key = "leaderboard:key";
        double score = 1000.0;
        String member = "player-uuid";

        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.zadd(key, score, member)).thenReturn(1L);

        injectMockJedisPool();

        CompletableFuture<Void> future = cacheProvider.zadd(key, score, member);
        future.get();

        verify(jedis).zadd(key, score, member);
        verify(jedis).close();
    }

    @Test
    @DisplayName("Zrevrange should retrieve sorted set members")
    void testZrevrange() throws Exception {
        String key = "leaderboard:key";
        Set<String> expectedMembers = new LinkedHashSet<>(Arrays.asList("player1", "player2", "player3"));

        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.zrevrange(key, 0, 9)).thenAnswer(invocation -> expectedMembers);

        injectMockJedisPool();

        CompletableFuture<List<String>> future = cacheProvider.zrevrange(key, 0, 9);
        List<String> result = future.get();

        assertEquals(3, result.size());
        assertTrue(result.contains("player1"));
        verify(jedis).zrevrange(key, 0, 9);
        verify(jedis).close();
    }

    @Test
    @DisplayName("Zrevrange with scores should retrieve members and scores")
    void testZrevrangeWithScores() throws Exception {
        String key = "leaderboard:key";
        List<Tuple> tuples = Arrays.asList(
            new Tuple("player1", 1000.0),
            new Tuple("player2", 900.0)
        );

        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.zrevrangeWithScores(key, 0, 9)).thenReturn(tuples);

        injectMockJedisPool();

        CompletableFuture<Map<String, Double>> future = cacheProvider.zrevrangeWithScores(key, 0, 9);
        Map<String, Double> result = future.get();

        assertEquals(2, result.size());
        assertEquals(1000.0, result.get("player1"));
        assertEquals(900.0, result.get("player2"));
        verify(jedis).zrevrangeWithScores(key, 0, 9);
        verify(jedis).close();
    }

    @Test
    @DisplayName("Hset should set hash field")
    void testHset() throws Exception {
        String key = "session:123";
        String field = "state";
        String value = "BETTING";

        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.hset(key, field, value)).thenReturn(1L);

        injectMockJedisPool();

        CompletableFuture<Void> future = cacheProvider.hset(key, field, value);
        future.get();

        verify(jedis).hset(key, field, value);
        verify(jedis).close();
    }

    @Test
    @DisplayName("Hget should retrieve hash field")
    void testHget() throws Exception {
        String key = "session:123";
        String field = "state";
        String expectedValue = "BETTING";

        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.hget(key, field)).thenReturn(expectedValue);

        injectMockJedisPool();

        CompletableFuture<Optional<String>> future = cacheProvider.hget(key, field);
        Optional<String> result = future.get();

        assertTrue(result.isPresent());
        assertEquals(expectedValue, result.get());
        verify(jedis).hget(key, field);
        verify(jedis).close();
    }

    @Test
    @DisplayName("Hget should return empty on missing field")
    void testHgetMissingField() throws Exception {
        String key = "session:123";
        String field = "missing";

        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.hget(key, field)).thenReturn(null);

        injectMockJedisPool();

        CompletableFuture<Optional<String>> future = cacheProvider.hget(key, field);
        Optional<String> result = future.get();

        assertFalse(result.isPresent());
        verify(jedis).hget(key, field);
    }

    @Test
    @DisplayName("HgetAll should retrieve all hash fields")
    void testHgetAll() throws Exception {
        String key = "session:123";
        Map<String, String> expectedData = Map.of(
            "state", "BETTING",
            "seed", "abc123",
            "hash", "def456"
        );

        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.hgetAll(key)).thenReturn(expectedData);

        injectMockJedisPool();

        CompletableFuture<Map<String, String>> future = cacheProvider.hgetAll(key);
        Map<String, String> result = future.get();

        assertEquals(3, result.size());
        assertEquals("BETTING", result.get("state"));
        assertEquals("abc123", result.get("seed"));
        verify(jedis).hgetAll(key);
        verify(jedis).close();
    }

    @Test
    @DisplayName("Hdel should delete hash field")
    void testHdel() throws Exception {
        String key = "session:123";
        String field = "state";

        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.hdel(key, field)).thenReturn(1L);

        injectMockJedisPool();

        CompletableFuture<Void> future = cacheProvider.hdel(key, field);
        future.get();

        verify(jedis).hdel(key, field);
        verify(jedis).close();
    }

    @Test
    @DisplayName("Get should handle errors gracefully")
    void testGetError() throws Exception {
        String key = "test:key";

        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.get(key)).thenThrow(new RuntimeException("Redis error"));

        injectMockJedisPool();

        CompletableFuture<Optional<String>> future = cacheProvider.get(key);
        Optional<String> result = future.get();

        assertFalse(result.isPresent());
        verify(jedis).close();
    }

    @Test
    @DisplayName("Cache type should be redis")
    void testCacheType() {
        assertEquals("redis", cacheProvider.getCacheType());
    }

    /**
     * Helper method to inject mock JedisPool using reflection.
     * This is needed because JedisPool is created in initialize() method.
     */
    private void injectMockJedisPool() {
        try {
            var field = RedisCacheProvider.class.getDeclaredField("jedisPool");
            field.setAccessible(true);
            field.set(cacheProvider, jedisPool);
        } catch (Exception e) {
            fail("Failed to inject mock JedisPool: " + e.getMessage());
        }
    }
}
