package vn.casino.core.database;

import com.zaxxer.hikari.HikariConfig;
import vn.casino.core.config.MainConfig;
import vn.casino.core.database.migrations.V1_InitialSchema;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * SQLite database provider implementation.
 * Suitable for development and single-server deployments.
 */
public class SQLiteProvider extends HikariManager {

    public SQLiteProvider(MainConfig config, Logger logger) {
        super(config, logger);
    }

    @Override
    protected HikariConfig createHikariConfig() {
        HikariConfig hikariConfig = new HikariConfig();

        // Connection settings
        hikariConfig.setJdbcUrl(config.getJdbcUrl());
        hikariConfig.setDriverClassName(config.getDriverClassName());

        // SQLite-specific pool settings (smaller pool for file-based DB)
        hikariConfig.setMaximumPoolSize(1); // SQLite works best with single connection
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setConnectionTimeout(config.getConnectionTimeout());
        hikariConfig.setIdleTimeout(config.getIdleTimeout());
        hikariConfig.setMaxLifetime(config.getMaxLifetime());

        // SQLite optimizations
        hikariConfig.addDataSourceProperty("journal_mode", "WAL");
        hikariConfig.addDataSourceProperty("synchronous", "NORMAL");
        hikariConfig.addDataSourceProperty("cache_size", "10000");
        hikariConfig.addDataSourceProperty("temp_store", "MEMORY");

        // Connection test
        hikariConfig.setConnectionTestQuery("SELECT 1");

        // Pool name
        hikariConfig.setPoolName("CasinoSQLite-Pool");

        return hikariConfig;
    }

    @Override
    protected void runMigrations() throws SQLException {
        logger.info("Running SQLite migrations...");

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Enable WAL mode for better concurrency
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA synchronous=NORMAL");
            stmt.execute("PRAGMA cache_size=10000");
            stmt.execute("PRAGMA temp_store=MEMORY");

            // Create migrations table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS casino_migrations (
                    version TEXT PRIMARY KEY,
                    applied_at INTEGER DEFAULT (strftime('%s', 'now'))
                )
            """);

            // Check if V1 already applied
            var rs = stmt.executeQuery("SELECT version FROM casino_migrations WHERE version = 'V1_InitialSchema'");
            if (rs.next()) {
                logger.info("Migration V1_InitialSchema already applied, skipping...");
                return;
            }

            // Run V1 migration
            V1_InitialSchema.applySQLite(conn);

            // Record migration
            stmt.execute("INSERT INTO casino_migrations (version) VALUES ('V1_InitialSchema')");

            logger.info("SQLite migrations completed successfully!");
        }
    }

    @Override
    public String getDatabaseType() {
        return "sqlite";
    }
}
