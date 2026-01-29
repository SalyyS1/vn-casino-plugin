package vn.casino.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import vn.casino.core.config.MainConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HikariCP connection pool manager.
 * Base implementation for all database providers.
 */
public abstract class HikariManager implements DatabaseProvider {

    protected final MainConfig config;
    protected final Logger logger;
    protected HikariDataSource dataSource;
    protected ExecutorService executor;

    public HikariManager(MainConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("Initializing " + getDatabaseType() + " database connection pool...");

                HikariConfig hikariConfig = createHikariConfig();
                this.dataSource = new HikariDataSource(hikariConfig);
                this.executor = Executors.newFixedThreadPool(
                        config.getAsyncPoolSize(),
                        r -> {
                            Thread thread = new Thread(r, "CasinoDB-Async");
                            thread.setDaemon(true);
                            return thread;
                        }
                );

                // Test connection
                try (Connection conn = dataSource.getConnection()) {
                    if (conn.isValid(5)) {
                        logger.info("Database connection established successfully!");
                    }
                }

                // Run migrations
                runMigrations();

                logger.info("Database initialized successfully!");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to initialize database", e);
                throw new RuntimeException("Database initialization failed", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            logger.info("Shutting down database connection pool...");

            if (executor != null && !executor.isShutdown()) {
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
            }

            logger.info("Database shutdown complete!");
        });
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Database connection pool is not initialized");
        }
        return dataSource.getConnection();
    }

    @Override
    public <T> CompletableFuture<T> queryAsync(String sql, Function<ResultSet, T> mapper, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                setParameters(stmt, params);

                try (ResultSet rs = stmt.executeQuery()) {
                    return mapper.apply(rs);
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Query failed: " + sql, e);
                throw new RuntimeException("Database query failed", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Integer> updateAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                setParameters(stmt, params);
                return stmt.executeUpdate();

            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Update failed: " + sql, e);
                throw new RuntimeException("Database update failed", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<int[]> batchUpdateAsync(String sql, Object[]... batchParams) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                for (Object[] params : batchParams) {
                    setParameters(stmt, params);
                    stmt.addBatch();
                }

                return stmt.executeBatch();

            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Batch update failed: " + sql, e);
                throw new RuntimeException("Database batch update failed", e);
            }
        }, executor);
    }

    @Override
    public boolean isHealthy() {
        try (Connection conn = getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Create HikariCP configuration for this database type.
     *
     * @return HikariConfig instance
     */
    protected abstract HikariConfig createHikariConfig();

    /**
     * Run database migrations.
     */
    protected abstract void runMigrations() throws SQLException;

    /**
     * Set prepared statement parameters.
     */
    private void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }
}
