package vn.casino.economy;

import vn.casino.core.database.DatabaseProvider;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repository for managing transaction records in the database.
 * Provides async operations for saving and querying transaction history.
 */
public class TransactionRepository {

    private final DatabaseProvider database;
    private final Logger logger;

    public TransactionRepository(DatabaseProvider database, Logger logger) {
        this.database = database;
        this.logger = logger;
    }

    /**
     * Save a transaction to the database.
     *
     * @param transaction Transaction to save
     * @return CompletableFuture with the saved transaction (with generated ID)
     */
    public CompletableFuture<Transaction> save(Transaction transaction) {
        String sql = """
            INSERT INTO casino_transactions
            (uuid, type, amount, balance_before, balance_after, game, session_id, description, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        return database.queryAsync(
            sql,
            rs -> {
                try {
                    if (rs.next()) {
                        long generatedId = rs.getLong(1);
                        return new Transaction(
                            generatedId,
                            transaction.uuid(),
                            transaction.type(),
                            transaction.amount(),
                            transaction.balanceBefore(),
                            transaction.balanceAfter(),
                            transaction.game(),
                            transaction.sessionId(),
                            transaction.description(),
                            transaction.createdAt()
                        );
                    }
                    return transaction;
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "Failed to get generated ID for transaction", e);
                    return transaction;
                }
            },
            transaction.uuid().toString(),
            transaction.type().name(),
            transaction.amount(),
            transaction.balanceBefore(),
            transaction.balanceAfter(),
            transaction.game(),
            transaction.sessionId(),
            transaction.description(),
            Timestamp.from(transaction.createdAt())
        ).exceptionally(ex -> {
            logger.log(Level.SEVERE, "Failed to save transaction", ex);
            return transaction;
        });
    }

    /**
     * Find transactions by player UUID with limit.
     *
     * @param playerUuid Player UUID
     * @param limit Maximum number of transactions to return
     * @return CompletableFuture with list of transactions (newest first)
     */
    public CompletableFuture<List<Transaction>> findByPlayer(UUID playerUuid, int limit) {
        String sql = """
            SELECT id, uuid, type, amount, balance_before, balance_after,
                   game, session_id, description, created_at
            FROM casino_transactions
            WHERE uuid = ?
            ORDER BY created_at DESC
            LIMIT ?
        """;

        return database.queryAsync(
            sql,
            this::mapResultSetToList,
            playerUuid.toString(),
            limit
        ).exceptionally(ex -> {
            logger.log(Level.SEVERE, "Failed to query transactions for player " + playerUuid, ex);
            return List.of();
        });
    }

    /**
     * Find transactions by game with limit.
     *
     * @param game Game identifier
     * @param limit Maximum number of transactions to return
     * @return CompletableFuture with list of transactions (newest first)
     */
    public CompletableFuture<List<Transaction>> findByGame(String game, int limit) {
        String sql = """
            SELECT id, uuid, type, amount, balance_before, balance_after,
                   game, session_id, description, created_at
            FROM casino_transactions
            WHERE game = ?
            ORDER BY created_at DESC
            LIMIT ?
        """;

        return database.queryAsync(
            sql,
            this::mapResultSetToList,
            game,
            limit
        ).exceptionally(ex -> {
            logger.log(Level.SEVERE, "Failed to query transactions for game " + game, ex);
            return List.of();
        });
    }

    /**
     * Delete transactions older than the specified duration.
     * Used for automatic cleanup of old transaction history.
     *
     * @param duration Duration threshold (e.g., 30 days)
     * @return CompletableFuture with number of deleted transactions
     */
    public CompletableFuture<Integer> deleteOlderThan(Duration duration) {
        Instant threshold = Instant.now().minus(duration);
        String sql = "DELETE FROM casino_transactions WHERE created_at < ?";

        return database.updateAsync(sql, Timestamp.from(threshold))
            .thenApply(deletedCount -> {
                if (deletedCount > 0) {
                    logger.info("Deleted " + deletedCount + " old transactions (older than " + duration.toDays() + " days)");
                }
                return deletedCount;
            })
            .exceptionally(ex -> {
                logger.log(Level.SEVERE, "Failed to delete old transactions", ex);
                return 0;
            });
    }

    /**
     * Get total transaction count for a player.
     *
     * @param playerUuid Player UUID
     * @return CompletableFuture with transaction count
     */
    public CompletableFuture<Integer> countByPlayer(UUID playerUuid) {
        String sql = "SELECT COUNT(*) FROM casino_transactions WHERE uuid = ?";

        return database.queryAsync(
            sql,
            rs -> {
                try {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                    return 0;
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "Failed to count transactions", e);
                    return 0;
                }
            },
            playerUuid.toString()
        ).exceptionally(ex -> {
            logger.log(Level.SEVERE, "Failed to count transactions for player " + playerUuid, ex);
            return 0;
        });
    }

    /**
     * Map ResultSet to list of transactions.
     */
    private List<Transaction> mapResultSetToList(ResultSet rs) {
        List<Transaction> transactions = new ArrayList<>();
        try {
            while (rs.next()) {
                transactions.add(mapResultSetToTransaction(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to map result set to transactions", e);
        }
        return transactions;
    }

    /**
     * Map single ResultSet row to Transaction.
     */
    private Transaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        return new Transaction(
            rs.getLong("id"),
            UUID.fromString(rs.getString("uuid")),
            TransactionType.valueOf(rs.getString("type")),
            rs.getBigDecimal("amount"),
            rs.getBigDecimal("balance_before"),
            rs.getBigDecimal("balance_after"),
            rs.getString("game"),
            rs.getObject("session_id", Long.class),
            rs.getString("description"),
            rs.getTimestamp("created_at").toInstant()
        );
    }
}
