package vn.casino.economy;

import vn.casino.core.cache.CacheProvider;
import vn.casino.core.database.DatabaseProvider;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Currency manager for VND casino economy.
 * Handles balance operations with Redis caching and transaction logging.
 * Thread-safe with optimistic locking for concurrent operations.
 */
public class CurrencyManager {

    private static final String BALANCE_CACHE_KEY = "casino:player:%s:balance";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final DatabaseProvider database;
    private final CacheProvider cache;
    private final TransactionRepository transactionRepository;
    private final Logger logger;

    // Per-player locks for thread-safe balance operations
    private final ConcurrentHashMap<UUID, Lock> playerLocks = new ConcurrentHashMap<>();

    public CurrencyManager(
        DatabaseProvider database,
        CacheProvider cache,
        TransactionRepository transactionRepository,
        Logger logger
    ) {
        this.database = database;
        this.cache = cache;
        this.transactionRepository = transactionRepository;
        this.logger = logger;
    }

    /**
     * Get player balance from cache or database.
     *
     * @param playerUuid Player UUID
     * @return CompletableFuture with current balance
     */
    public CompletableFuture<BigDecimal> getBalance(UUID playerUuid) {
        String cacheKey = String.format(BALANCE_CACHE_KEY, playerUuid);

        // Try cache first
        return cache.get(cacheKey)
            .thenCompose(cachedBalance -> {
                if (cachedBalance.isPresent()) {
                    try {
                        return CompletableFuture.completedFuture(new BigDecimal(cachedBalance.get()));
                    } catch (NumberFormatException e) {
                        logger.log(Level.WARNING, "Invalid cached balance for " + playerUuid, e);
                        // Fall through to database
                    }
                }

                // Cache miss - fetch from database
                return getBalanceFromDatabase(playerUuid)
                    .thenApply(balance -> {
                        // Update cache asynchronously
                        cacheBalance(playerUuid, balance);
                        return balance;
                    });
            })
            .exceptionally(ex -> {
                logger.log(Level.SEVERE, "Failed to get balance for " + playerUuid, ex);
                return ZERO;
            });
    }

    /**
     * Check if player has sufficient balance.
     *
     * @param playerUuid Player UUID
     * @param amount Required amount
     * @return CompletableFuture with true if player has enough balance
     */
    public CompletableFuture<Boolean> hasBalance(UUID playerUuid, BigDecimal amount) {
        return getBalance(playerUuid)
            .thenApply(balance -> balance.compareTo(amount) >= 0);
    }

    /**
     * Deposit amount to player balance.
     * Creates player record if doesn't exist.
     *
     * @param playerUuid Player UUID
     * @param amount Amount to deposit (must be positive)
     * @param type Transaction type
     * @param game Game identifier (nullable)
     * @param sessionId Session ID (nullable)
     * @return CompletableFuture with new balance after deposit
     */
    public CompletableFuture<BigDecimal> deposit(
        UUID playerUuid,
        BigDecimal amount,
        TransactionType type,
        String game,
        Long sessionId
    ) {
        return deposit(playerUuid, amount, type, game, sessionId, null);
    }

    /**
     * Deposit amount to player balance with description.
     *
     * @param playerUuid Player UUID
     * @param amount Amount to deposit (must be positive)
     * @param type Transaction type
     * @param game Game identifier (nullable)
     * @param sessionId Session ID (nullable)
     * @param description Transaction description (nullable)
     * @return CompletableFuture with new balance after deposit
     */
    public CompletableFuture<BigDecimal> deposit(
        UUID playerUuid,
        BigDecimal amount,
        TransactionType type,
        String game,
        Long sessionId,
        String description
    ) {
        if (amount.compareTo(ZERO) <= 0) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Deposit amount must be positive")
            );
        }

        return executeBalanceOperation(playerUuid, amount, type, game, sessionId, description, true);
    }

    /**
     * Withdraw amount from player balance.
     * Fails if insufficient balance.
     *
     * @param playerUuid Player UUID
     * @param amount Amount to withdraw (must be positive)
     * @param type Transaction type
     * @param game Game identifier (nullable)
     * @param sessionId Session ID (nullable)
     * @return CompletableFuture with new balance after withdrawal, or failed future if insufficient
     */
    public CompletableFuture<BigDecimal> withdraw(
        UUID playerUuid,
        BigDecimal amount,
        TransactionType type,
        String game,
        Long sessionId
    ) {
        return withdraw(playerUuid, amount, type, game, sessionId, null);
    }

    /**
     * Withdraw amount from player balance with description.
     *
     * @param playerUuid Player UUID
     * @param amount Amount to withdraw (must be positive)
     * @param type Transaction type
     * @param game Game identifier (nullable)
     * @param sessionId Session ID (nullable)
     * @param description Transaction description (nullable)
     * @return CompletableFuture with new balance after withdrawal, or failed future if insufficient
     */
    public CompletableFuture<BigDecimal> withdraw(
        UUID playerUuid,
        BigDecimal amount,
        TransactionType type,
        String game,
        Long sessionId,
        String description
    ) {
        if (amount.compareTo(ZERO) <= 0) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Withdraw amount must be positive")
            );
        }

        return executeBalanceOperation(playerUuid, amount, type, game, sessionId, description, false);
    }

    /**
     * Execute balance operation (deposit or withdraw) with transaction logging.
     * Uses database transaction for atomicity.
     */
    private CompletableFuture<BigDecimal> executeBalanceOperation(
        UUID playerUuid,
        BigDecimal amount,
        TransactionType type,
        String game,
        Long sessionId,
        String description,
        boolean isDeposit
    ) {
        Lock lock = playerLocks.computeIfAbsent(playerUuid, k -> new ReentrantLock());

        return CompletableFuture.supplyAsync(() -> {
            lock.lock();
            try {
                return executeAtomicBalanceUpdate(
                    playerUuid, amount, type, game, sessionId, description, isDeposit
                );
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Balance operation failed for " + playerUuid, e);
                throw new RuntimeException("Balance operation failed", e);
            } finally {
                lock.unlock();
            }
        });
    }

    /**
     * Execute atomic balance update within database transaction.
     */
    private BigDecimal executeAtomicBalanceUpdate(
        UUID playerUuid,
        BigDecimal amount,
        TransactionType type,
        String game,
        Long sessionId,
        String description,
        boolean isDeposit
    ) throws SQLException {
        try (Connection conn = database.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Get or create player balance
                BigDecimal balanceBefore = getOrCreatePlayerBalance(conn, playerUuid);

                // Calculate new balance
                BigDecimal balanceAfter = isDeposit
                    ? balanceBefore.add(amount)
                    : balanceBefore.subtract(amount);

                // Check for negative balance on withdrawal
                if (!isDeposit && balanceAfter.compareTo(ZERO) < 0) {
                    conn.rollback();
                    throw new IllegalStateException("Insufficient balance");
                }

                // Update player balance
                updatePlayerBalance(conn, playerUuid, balanceAfter);

                // Log transaction
                logTransaction(conn, playerUuid, type, isDeposit ? amount : amount.negate(),
                    balanceBefore, balanceAfter, game, sessionId, description);

                // Commit transaction
                conn.commit();

                // Invalidate cache
                invalidateCache(playerUuid);

                logger.fine("Balance operation successful: " + playerUuid +
                    " " + type + " " + amount + " (new balance: " + balanceAfter + ")");

                return balanceAfter;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Get player balance from database, or create new player with 0 balance.
     */
    private BigDecimal getOrCreatePlayerBalance(Connection conn, UUID playerUuid) throws SQLException {
        String selectSql = "SELECT balance FROM casino_players WHERE uuid = ?";

        try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            stmt.setString(1, playerUuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("balance");
                }
            }
        }

        // Player doesn't exist - create with 0 balance
        String insertSql = "INSERT INTO casino_players (uuid, balance) VALUES (?, 0.00)";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            stmt.setString(1, playerUuid.toString());
            stmt.executeUpdate();
        }

        return ZERO;
    }

    /**
     * Update player balance in database.
     */
    private void updatePlayerBalance(Connection conn, UUID playerUuid, BigDecimal newBalance) throws SQLException {
        String sql = "UPDATE casino_players SET balance = ?, updated_at = CURRENT_TIMESTAMP WHERE uuid = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, newBalance);
            stmt.setString(2, playerUuid.toString());
            stmt.executeUpdate();
        }
    }

    /**
     * Log transaction to database.
     */
    private void logTransaction(
        Connection conn,
        UUID playerUuid,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String game,
        Long sessionId,
        String description
    ) throws SQLException {
        String sql = """
            INSERT INTO casino_transactions
            (uuid, type, amount, balance_before, balance_after, game, session_id, description)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, type.name());
            stmt.setBigDecimal(3, amount);
            stmt.setBigDecimal(4, balanceBefore);
            stmt.setBigDecimal(5, balanceAfter);
            stmt.setString(6, game);
            if (sessionId != null) {
                stmt.setLong(7, sessionId);
            } else {
                stmt.setNull(7, java.sql.Types.BIGINT);
            }
            stmt.setString(8, description);
            stmt.executeUpdate();
        }
    }

    /**
     * Get balance from database (bypasses cache).
     */
    private CompletableFuture<BigDecimal> getBalanceFromDatabase(UUID playerUuid) {
        String sql = "SELECT balance FROM casino_players WHERE uuid = ?";

        return database.queryAsync(
            sql,
            rs -> {
                try {
                    if (rs.next()) {
                        return rs.getBigDecimal("balance");
                    }
                    return ZERO;
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "Failed to read balance from database", e);
                    return ZERO;
                }
            },
            playerUuid.toString()
        );
    }

    /**
     * Cache player balance in Redis.
     *
     * @param playerUuid Player UUID
     * @param balance Balance to cache
     */
    public void cacheBalance(UUID playerUuid, BigDecimal balance) {
        String cacheKey = String.format(BALANCE_CACHE_KEY, playerUuid);
        cache.set(cacheKey, balance.toPlainString(), CACHE_TTL)
            .exceptionally(ex -> {
                logger.log(Level.WARNING, "Failed to cache balance for " + playerUuid, ex);
                return null;
            });
    }

    /**
     * Invalidate cached balance for player.
     *
     * @param playerUuid Player UUID
     */
    public void invalidateCache(UUID playerUuid) {
        String cacheKey = String.format(BALANCE_CACHE_KEY, playerUuid);
        cache.delete(cacheKey)
            .exceptionally(ex -> {
                logger.log(Level.WARNING, "Failed to invalidate cache for " + playerUuid, ex);
                return null;
            });
    }

    /**
     * Set player balance directly (admin command).
     * Creates transaction log with ADMIN_GIVE or ADMIN_TAKE type.
     *
     * @param playerUuid Player UUID
     * @param newBalance New balance to set
     * @param reason Reason for balance change
     * @return CompletableFuture with new balance
     */
    public CompletableFuture<BigDecimal> setBalance(UUID playerUuid, BigDecimal newBalance, String reason) {
        if (newBalance.compareTo(ZERO) < 0) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Balance cannot be negative")
            );
        }

        return getBalance(playerUuid).thenCompose(currentBalance -> {
            BigDecimal difference = newBalance.subtract(currentBalance);

            if (difference.compareTo(ZERO) > 0) {
                // Increase balance
                return deposit(playerUuid, difference, TransactionType.ADMIN_GIVE, null, null, reason);
            } else if (difference.compareTo(ZERO) < 0) {
                // Decrease balance
                return withdraw(playerUuid, difference.abs(), TransactionType.ADMIN_TAKE, null, null, reason);
            } else {
                // No change
                return CompletableFuture.completedFuture(currentBalance);
            }
        });
    }

    /**
     * Clean up player lock to prevent memory leak.
     * Call this when player logs out.
     *
     * @param playerUuid Player UUID
     */
    public void cleanupPlayerLock(UUID playerUuid) {
        playerLocks.remove(playerUuid);
    }
}
