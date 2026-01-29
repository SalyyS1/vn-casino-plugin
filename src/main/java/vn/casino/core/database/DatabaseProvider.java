package vn.casino.core.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Database provider interface for async database operations.
 * Supports MySQL, PostgreSQL, and SQLite backends.
 */
public interface DatabaseProvider {

    /**
     * Initialize the database connection pool and run migrations.
     *
     * @return CompletableFuture that completes when initialization is done
     */
    CompletableFuture<Void> initialize();

    /**
     * Shutdown the database connection pool gracefully.
     *
     * @return CompletableFuture that completes when shutdown is done
     */
    CompletableFuture<Void> shutdown();

    /**
     * Get a connection from the pool (synchronous).
     * Use this only for synchronous contexts.
     *
     * @return Database connection
     * @throws SQLException if connection fails
     */
    Connection getConnection() throws SQLException;

    /**
     * Execute an async SELECT query and map the result.
     *
     * @param sql SQL query with ? placeholders
     * @param mapper Function to map ResultSet to return type
     * @param params Query parameters
     * @param <T> Return type
     * @return CompletableFuture with mapped result
     */
    <T> CompletableFuture<T> queryAsync(String sql, Function<ResultSet, T> mapper, Object... params);

    /**
     * Execute an async INSERT/UPDATE/DELETE query.
     *
     * @param sql SQL query with ? placeholders
     * @param params Query parameters
     * @return CompletableFuture with affected row count
     */
    CompletableFuture<Integer> updateAsync(String sql, Object... params);

    /**
     * Execute an async query without expecting a return value.
     * Convenience method for updates that don't need result count.
     *
     * @param sql SQL query with ? placeholders
     * @param params Query parameters
     * @return CompletableFuture that completes when done
     */
    default CompletableFuture<Void> executeAsync(String sql, Object... params) {
        return updateAsync(sql, params).thenApply(count -> null);
    }

    /**
     * Execute an async batch UPDATE query.
     *
     * @param sql SQL query with ? placeholders
     * @param batchParams List of parameter arrays for batch execution
     * @return CompletableFuture with array of affected row counts
     */
    CompletableFuture<int[]> batchUpdateAsync(String sql, Object[]... batchParams);

    /**
     * Check if the database connection is healthy.
     *
     * @return true if database is reachable
     */
    boolean isHealthy();

    /**
     * Get the database type (mysql, postgresql, sqlite).
     *
     * @return database type identifier
     */
    String getDatabaseType();
}
