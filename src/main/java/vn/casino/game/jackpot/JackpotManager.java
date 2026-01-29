package vn.casino.game.jackpot;

import vn.casino.core.cache.CacheProvider;
import vn.casino.core.database.DatabaseProvider;
import vn.casino.economy.CurrencyManager;
import vn.casino.economy.TransactionType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages jackpot pools for all casino games.
 * Handles contributions, trigger checks, and winner payouts.
 * Uses Redis for real-time pool tracking with DB persistence.
 */
public class JackpotManager {

    private static final String JACKPOT_CACHE_KEY = "casino:jackpot:%s"; // %s = gameId
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final DatabaseProvider database;
    private final CacheProvider cache;
    private final CurrencyManager currencyManager;
    private final Logger logger;

    // Per-game jackpot configs
    private final Map<String, JackpotConfig> configs = new HashMap<>();

    public JackpotManager(
        DatabaseProvider database,
        CacheProvider cache,
        CurrencyManager currencyManager,
        Logger logger
    ) {
        this.database = database;
        this.cache = cache;
        this.currencyManager = currencyManager;
        this.logger = logger;

        // Initialize default configs
        initializeDefaultConfigs();
    }

    /**
     * Initialize default jackpot configurations for all games.
     */
    private void initializeDefaultConfigs() {
        JackpotConfig defaultConfig = JackpotConfig.createDefault();
        configs.put("taixiu", defaultConfig);
        configs.put("xocdia", defaultConfig);
        configs.put("baucua", defaultConfig);

        logger.info("Initialized jackpot configs (0.2% contribution, 0.001% base trigger)");
    }

    /**
     * Set custom jackpot configuration for a game.
     *
     * @param gameId Game identifier
     * @param config Jackpot configuration
     */
    public void setConfig(String gameId, JackpotConfig config) {
        configs.put(gameId, config);
        logger.info("Updated jackpot config for " + gameId);
    }

    /**
     * Get current jackpot pool for a game.
     *
     * @param gameId Game identifier
     * @return CompletableFuture with current pool amount
     */
    public CompletableFuture<BigDecimal> getPool(String gameId) {
        String cacheKey = String.format(JACKPOT_CACHE_KEY, gameId);

        return cache.get(cacheKey)
            .thenCompose(cached -> {
                if (cached.isPresent()) {
                    try {
                        return CompletableFuture.completedFuture(new BigDecimal(cached.get()));
                    } catch (NumberFormatException e) {
                        logger.warning("Invalid cached jackpot for " + gameId);
                    }
                }

                // Cache miss - load from database
                return loadPoolFromDatabase(gameId)
                    .thenApply(pool -> {
                        cachePool(gameId, pool);
                        return pool;
                    });
            })
            .exceptionally(ex -> {
                logger.log(Level.SEVERE, "Failed to get jackpot pool for " + gameId, ex);
                return BigDecimal.ZERO;
            });
    }

    /**
     * Contribute to jackpot pool from a bet.
     *
     * @param gameId Game identifier
     * @param betAmount Bet amount
     */
    public void contribute(String gameId, BigDecimal betAmount) {
        JackpotConfig config = configs.get(gameId);
        if (config == null) {
            logger.warning("No jackpot config for " + gameId);
            return;
        }

        BigDecimal contribution = config.calculateContribution(betAmount);
        if (contribution.compareTo(BigDecimal.ZERO) <= 0) {
            return; // No contribution
        }

        getPool(gameId)
            .thenCompose(currentPool -> {
                BigDecimal newPool = currentPool.add(contribution);
                return updatePool(gameId, newPool);
            })
            .exceptionally(ex -> {
                logger.log(Level.WARNING, "Failed to contribute to jackpot " + gameId, ex);
                return null;
            });
    }

    /**
     * Check if jackpot should trigger and select winner.
     *
     * @param gameId Game identifier
     * @param participants Set of player UUIDs who can win
     * @return CompletableFuture with winner UUID (empty if no trigger)
     */
    public CompletableFuture<Optional<UUID>> checkTrigger(String gameId, Set<UUID> participants) {
        if (participants.isEmpty()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        JackpotConfig config = configs.get(gameId);
        if (config == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return getPool(gameId)
            .thenCompose(currentPool -> {
                // Check if pool meets minimum
                if (currentPool.compareTo(config.minJackpot()) < 0) {
                    return CompletableFuture.completedFuture(Optional.empty());
                }

                // Calculate trigger chance
                double triggerChance = config.calculateTriggerChance(currentPool);
                double roll = ThreadLocalRandom.current().nextDouble();

                if (roll < triggerChance) {
                    // Jackpot triggered! Select random winner
                    UUID winner = selectRandomWinner(participants);
                    return triggerJackpot(gameId, winner, currentPool)
                        .thenApply(success -> success ? Optional.of(winner) : Optional.empty());
                }

                return CompletableFuture.completedFuture(Optional.empty());
            });
    }

    /**
     * Trigger jackpot payout to winner.
     *
     * @param gameId Game identifier
     * @param winnerId Winner UUID
     * @param amount Jackpot amount
     * @return CompletableFuture with success status
     */
    private CompletableFuture<Boolean> triggerJackpot(String gameId, UUID winnerId, BigDecimal amount) {
        JackpotConfig config = configs.get(gameId);

        logger.info("JACKPOT TRIGGERED! Game: " + gameId + ", Winner: " + winnerId +
            ", Amount: " + amount);

        // Pay winner
        return currencyManager.deposit(
            winnerId,
            amount,
            TransactionType.JACKPOT,
            gameId,
            null,
            "Jackpot win (" + gameId + ")"
        ).thenCompose(newBalance -> {
            // Reset pool to seed amount
            BigDecimal seedAmount = config != null ? config.seedAmount() : new BigDecimal("10000");
            return updatePool(gameId, seedAmount)
                .thenApply(v -> {
                    // Log jackpot win to database
                    logJackpotWin(gameId, winnerId, amount);
                    return true;
                });
        }).exceptionally(ex -> {
            logger.log(Level.SEVERE, "Failed to trigger jackpot for " + gameId, ex);
            return false;
        });
    }

    /**
     * Update jackpot pool (cache + database).
     *
     * @param gameId Game identifier
     * @param newPool New pool amount
     * @return CompletableFuture when complete
     */
    private CompletableFuture<Void> updatePool(String gameId, BigDecimal newPool) {
        // Update cache
        cachePool(gameId, newPool);

        // Update database
        String sql = """
            INSERT INTO casino_jackpots (game_id, pool_amount, updated_at)
            VALUES (?, ?, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE pool_amount = ?, updated_at = CURRENT_TIMESTAMP
        """;

        return database.executeAsync(sql, gameId, newPool, newPool)
            .thenRun(() -> {
                logger.fine("Updated jackpot pool for " + gameId + " to " + newPool);
            });
    }

    /**
     * Load jackpot pool from database.
     *
     * @param gameId Game identifier
     * @return CompletableFuture with pool amount
     */
    private CompletableFuture<BigDecimal> loadPoolFromDatabase(String gameId) {
        String sql = "SELECT pool_amount FROM casino_jackpots WHERE game_id = ?";

        return database.queryAsync(
            sql,
            rs -> {
                try {
                    if (rs.next()) {
                        return rs.getBigDecimal("pool_amount");
                    }
                    // No record - initialize with seed amount
                    JackpotConfig config = configs.get(gameId);
                    BigDecimal seedAmount = config != null ? config.seedAmount() : new BigDecimal("10000");
                    initializePool(gameId, seedAmount);
                    return seedAmount;
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "Failed to load jackpot pool from DB", e);
                    return BigDecimal.ZERO;
                }
            },
            gameId
        );
    }

    /**
     * Initialize jackpot pool in database.
     *
     * @param gameId Game identifier
     * @param seedAmount Initial amount
     */
    private void initializePool(String gameId, BigDecimal seedAmount) {
        String sql = "INSERT INTO casino_jackpots (game_id, pool_amount) VALUES (?, ?)";
        database.executeAsync(sql, gameId, seedAmount)
            .exceptionally(ex -> {
                logger.log(Level.WARNING, "Failed to initialize jackpot pool", ex);
                return null;
            });
    }

    /**
     * Cache jackpot pool value.
     *
     * @param gameId Game identifier
     * @param amount Pool amount
     */
    private void cachePool(String gameId, BigDecimal amount) {
        String cacheKey = String.format(JACKPOT_CACHE_KEY, gameId);
        cache.set(cacheKey, amount.toPlainString(), CACHE_TTL)
            .exceptionally(ex -> {
                logger.log(Level.WARNING, "Failed to cache jackpot pool", ex);
                return null;
            });
    }

    /**
     * Log jackpot win to database.
     *
     * @param gameId Game identifier
     * @param winnerId Winner UUID
     * @param amount Jackpot amount
     */
    private void logJackpotWin(String gameId, UUID winnerId, BigDecimal amount) {
        String sql = """
            INSERT INTO casino_jackpot_wins (game_id, winner_uuid, amount, won_at)
            VALUES (?, ?, ?, CURRENT_TIMESTAMP)
        """;

        database.executeAsync(sql, gameId, winnerId.toString(), amount)
            .exceptionally(ex -> {
                logger.log(Level.WARNING, "Failed to log jackpot win", ex);
                return null;
            });
    }

    /**
     * Select random winner from participants.
     *
     * @param participants Set of player UUIDs
     * @return Random winner UUID
     */
    private UUID selectRandomWinner(Set<UUID> participants) {
        List<UUID> players = new ArrayList<>(participants);
        int randomIndex = ThreadLocalRandom.current().nextInt(players.size());
        return players.get(randomIndex);
    }

    /**
     * Get jackpot statistics for a game.
     *
     * @param gameId Game identifier
     * @return Map with pool, trigger chance, etc.
     */
    public CompletableFuture<Map<String, Object>> getStats(String gameId) {
        return getPool(gameId).thenApply(pool -> {
            JackpotConfig config = configs.get(gameId);
            Map<String, Object> stats = new HashMap<>();
            stats.put("pool", pool);
            stats.put("triggerChance", config != null ? config.calculateTriggerChance(pool) : 0.0);
            stats.put("minJackpot", config != null ? config.minJackpot() : BigDecimal.ZERO);
            return stats;
        });
    }
}
