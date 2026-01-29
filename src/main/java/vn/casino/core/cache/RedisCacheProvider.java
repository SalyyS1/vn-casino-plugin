package vn.casino.core.cache;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.resps.Tuple;
import vn.casino.core.config.MainConfig;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Redis cache provider implementation using Jedis.
 * High-performance caching for sessions, leaderboards, and jackpots.
 */
public class RedisCacheProvider implements CacheProvider {

    private final MainConfig config;
    private final Logger logger;
    private JedisPool jedisPool;
    private ExecutorService executor;

    public RedisCacheProvider(MainConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("Initializing Redis cache connection...");

                JedisPoolConfig poolConfig = new JedisPoolConfig();
                poolConfig.setMaxTotal(config.getRedisPoolSize());
                poolConfig.setMaxIdle(config.getRedisPoolSize() / 2);
                poolConfig.setMinIdle(2);
                poolConfig.setTestOnBorrow(true);
                poolConfig.setTestOnReturn(true);
                poolConfig.setTestWhileIdle(true);

                if (config.getRedisPassword() != null && !config.getRedisPassword().isEmpty()) {
                    this.jedisPool = new JedisPool(
                            poolConfig,
                            config.getRedisHost(),
                            config.getRedisPort(),
                            config.getRedisTimeout(),
                            config.getRedisPassword(),
                            config.getRedisDatabase()
                    );
                } else {
                    this.jedisPool = new JedisPool(
                            poolConfig,
                            config.getRedisHost(),
                            config.getRedisPort(),
                            config.getRedisTimeout()
                    );
                }

                this.executor = Executors.newFixedThreadPool(
                        4,
                        r -> {
                            Thread thread = new Thread(r, "CasinoCache-Async");
                            thread.setDaemon(true);
                            return thread;
                        }
                );

                // Test connection
                try (Jedis jedis = jedisPool.getResource()) {
                    String pong = jedis.ping();
                    if ("PONG".equals(pong)) {
                        logger.info("Redis cache connected successfully!");
                    }
                }

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to initialize Redis cache", e);
                throw new RuntimeException("Redis cache initialization failed", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            logger.info("Shutting down Redis cache connection...");

            if (executor != null && !executor.isShutdown()) {
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            if (jedisPool != null && !jedisPool.isClosed()) {
                jedisPool.close();
            }

            logger.info("Redis cache shutdown complete!");
        });
    }

    @Override
    public CompletableFuture<Void> set(String key, String value, Duration ttl) {
        return CompletableFuture.runAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.setex(key, ttl.toSeconds(), value);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Redis SET failed: " + key, e);
                throw new RuntimeException("Redis operation failed", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<String>> get(String key) {
        return CompletableFuture.supplyAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                return Optional.ofNullable(jedis.get(key));
            } catch (Exception e) {
                logger.log(Level.WARNING, "Redis GET failed: " + key, e);
                return Optional.empty();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> delete(String key) {
        return CompletableFuture.runAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.del(key);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Redis DEL failed: " + key, e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Long> increment(String key) {
        return CompletableFuture.supplyAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                return jedis.incr(key);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Redis INCR failed: " + key, e);
                throw new RuntimeException("Redis operation failed", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Long> decrement(String key) {
        return CompletableFuture.supplyAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                return jedis.decr(key);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Redis DECR failed: " + key, e);
                throw new RuntimeException("Redis operation failed", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> zadd(String key, double score, String member) {
        return CompletableFuture.runAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.zadd(key, score, member);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Redis ZADD failed: " + key, e);
                throw new RuntimeException("Redis operation failed", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<String>> zrevrange(String key, long start, long stop) {
        return CompletableFuture.supplyAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                return new ArrayList<>(jedis.zrevrange(key, start, stop));
            } catch (Exception e) {
                logger.log(Level.WARNING, "Redis ZREVRANGE failed: " + key, e);
                return Collections.emptyList();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Map<String, Double>> zrevrangeWithScores(String key, long start, long stop) {
        return CompletableFuture.supplyAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                Map<String, Double> result = jedis.zrevrangeWithScores(key, start, stop).stream()
                        .collect(Collectors.toMap(
                                Tuple::getElement,
                                Tuple::getScore,
                                (a, b) -> a,
                                LinkedHashMap::new
                        ));
                return result;
            } catch (Exception e) {
                logger.log(Level.WARNING, "Redis ZREVRANGE_WITHSCORES failed: " + key, e);
                return Collections.emptyMap();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> hset(String key, String field, String value) {
        return CompletableFuture.runAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.hset(key, field, value);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Redis HSET failed: " + key, e);
                throw new RuntimeException("Redis operation failed", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<String>> hget(String key, String field) {
        return CompletableFuture.supplyAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                return Optional.ofNullable(jedis.hget(key, field));
            } catch (Exception e) {
                logger.log(Level.WARNING, "Redis HGET failed: " + key, e);
                return Optional.empty();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Map<String, String>> hgetAll(String key) {
        return CompletableFuture.supplyAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                return new HashMap<>(jedis.hgetAll(key));
            } catch (Exception e) {
                logger.log(Level.WARNING, "Redis HGETALL failed: " + key, e);
                return Collections.emptyMap();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> hdel(String key, String field) {
        return CompletableFuture.runAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.hdel(key, field);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Redis HDEL failed: " + key, e);
            }
        }, executor);
    }

    @Override
    public boolean isHealthy() {
        try (Jedis jedis = jedisPool.getResource()) {
            return "PONG".equals(jedis.ping());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getCacheType() {
        return "redis";
    }
}
