package vn.casino.core.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import vn.casino.core.config.MainConfig;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Caffeine in-memory cache fallback implementation.
 * Used when Redis is unavailable or disabled.
 *
 * WARNING: This is a local cache and does not sync across servers.
 * Only suitable for single-server deployments or development.
 */
public class CaffeineFallback implements CacheProvider {

    private final MainConfig config;
    private final Logger logger;
    private Cache<String, CacheEntry> cache;
    private final Map<String, ConcurrentHashMap<String, Double>> sortedSets = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentHashMap<String, String>> hashes = new ConcurrentHashMap<>();
    private ExecutorService executor;

    public CaffeineFallback(MainConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            logger.warning("Using Caffeine local cache fallback (Redis unavailable)");
            logger.warning("Cache data will NOT be shared across servers!");

            this.cache = Caffeine.newBuilder()
                    .maximumSize(config.getMaxCachedPlayers() * 10L)
                    .expireAfterWrite(Duration.ofSeconds(config.getPlayerCacheDuration()))
                    .build();

            this.executor = Executors.newFixedThreadPool(
                    2,
                    r -> {
                        Thread thread = new Thread(r, "CasinoCaffeine-Async");
                        thread.setDaemon(true);
                        return thread;
                    }
            );

            logger.info("Caffeine cache initialized successfully!");
        });
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            logger.info("Shutting down Caffeine cache...");

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

            if (cache != null) {
                cache.invalidateAll();
            }
            sortedSets.clear();
            hashes.clear();

            logger.info("Caffeine cache shutdown complete!");
        });
    }

    @Override
    public CompletableFuture<Void> set(String key, String value, Duration ttl) {
        return CompletableFuture.runAsync(() -> {
            cache.put(key, new CacheEntry(value, System.currentTimeMillis() + ttl.toMillis()));
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<String>> get(String key) {
        return CompletableFuture.supplyAsync(() -> {
            CacheEntry entry = cache.getIfPresent(key);
            if (entry == null) {
                return Optional.empty();
            }

            // Check if expired
            if (entry.expiresAt > 0 && System.currentTimeMillis() > entry.expiresAt) {
                cache.invalidate(key);
                return Optional.empty();
            }

            return Optional.of(entry.value);
        }, executor);
    }

    @Override
    public CompletableFuture<Void> delete(String key) {
        return CompletableFuture.runAsync(() -> {
            cache.invalidate(key);
        }, executor);
    }

    @Override
    public CompletableFuture<Long> increment(String key) {
        return CompletableFuture.supplyAsync(() -> {
            CacheEntry entry = cache.getIfPresent(key);
            long value = 1;

            if (entry != null) {
                try {
                    value = Long.parseLong(entry.value) + 1;
                } catch (NumberFormatException e) {
                    value = 1;
                }
            }

            cache.put(key, new CacheEntry(String.valueOf(value), 0));
            return value;
        }, executor);
    }

    @Override
    public CompletableFuture<Long> decrement(String key) {
        return CompletableFuture.supplyAsync(() -> {
            CacheEntry entry = cache.getIfPresent(key);
            long value = -1;

            if (entry != null) {
                try {
                    value = Long.parseLong(entry.value) - 1;
                } catch (NumberFormatException e) {
                    value = -1;
                }
            }

            cache.put(key, new CacheEntry(String.valueOf(value), 0));
            return value;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> zadd(String key, double score, String member) {
        return CompletableFuture.runAsync(() -> {
            sortedSets.computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                    .put(member, score);
        }, executor);
    }

    @Override
    public CompletableFuture<List<String>> zrevrange(String key, long start, long stop) {
        return CompletableFuture.supplyAsync(() -> {
            ConcurrentHashMap<String, Double> set = sortedSets.get(key);
            if (set == null || set.isEmpty()) {
                return Collections.emptyList();
            }

            List<String> sorted = set.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            int fromIndex = (int) Math.max(0, start);
            int toIndex = stop == -1 ? sorted.size() : (int) Math.min(sorted.size(), stop + 1);

            if (fromIndex >= sorted.size()) {
                return Collections.emptyList();
            }

            return sorted.subList(fromIndex, toIndex);
        }, executor);
    }

    @Override
    public CompletableFuture<Map<String, Double>> zrevrangeWithScores(String key, long start, long stop) {
        return CompletableFuture.supplyAsync(() -> {
            ConcurrentHashMap<String, Double> set = sortedSets.get(key);
            if (set == null || set.isEmpty()) {
                return Collections.emptyMap();
            }

            List<Map.Entry<String, Double>> sorted = set.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .collect(Collectors.toList());

            int fromIndex = (int) Math.max(0, start);
            int toIndex = stop == -1 ? sorted.size() : (int) Math.min(sorted.size(), stop + 1);

            if (fromIndex >= sorted.size()) {
                return Collections.emptyMap();
            }

            return sorted.subList(fromIndex, toIndex).stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (a, b) -> a,
                            LinkedHashMap::new
                    ));
        }, executor);
    }

    @Override
    public CompletableFuture<Void> hset(String key, String field, String value) {
        return CompletableFuture.runAsync(() -> {
            hashes.computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                    .put(field, value);
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<String>> hget(String key, String field) {
        return CompletableFuture.supplyAsync(() -> {
            ConcurrentHashMap<String, String> hash = hashes.get(key);
            if (hash == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(hash.get(field));
        }, executor);
    }

    @Override
    public CompletableFuture<Map<String, String>> hgetAll(String key) {
        return CompletableFuture.supplyAsync(() -> {
            ConcurrentHashMap<String, String> hash = hashes.get(key);
            if (hash == null) {
                return Collections.emptyMap();
            }
            return new HashMap<>(hash);
        }, executor);
    }

    @Override
    public CompletableFuture<Void> hdel(String key, String field) {
        return CompletableFuture.runAsync(() -> {
            ConcurrentHashMap<String, String> hash = hashes.get(key);
            if (hash != null) {
                hash.remove(field);
            }
        }, executor);
    }

    @Override
    public boolean isHealthy() {
        return cache != null;
    }

    @Override
    public String getCacheType() {
        return "caffeine";
    }

    /**
     * Cache entry with expiration support.
     */
    private static class CacheEntry {
        final String value;
        final long expiresAt;

        CacheEntry(String value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }
    }
}
