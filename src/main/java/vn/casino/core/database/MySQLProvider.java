package vn.casino.core.database;

import com.zaxxer.hikari.HikariConfig;
import vn.casino.core.config.MainConfig;
import vn.casino.core.database.migrations.V1_InitialSchema;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * MySQL database provider implementation.
 * Optimized for production use with MySQL/MariaDB.
 */
public class MySQLProvider extends HikariManager {

    public MySQLProvider(MainConfig config, Logger logger) {
        super(config, logger);
    }

    @Override
    protected HikariConfig createHikariConfig() {
        HikariConfig hikariConfig = new HikariConfig();

        // Connection settings
        hikariConfig.setJdbcUrl(config.getJdbcUrl());
        hikariConfig.setDriverClassName(config.getDriverClassName());
        hikariConfig.setUsername(config.getDatabaseUsername());
        hikariConfig.setPassword(config.getDatabasePassword());

        // Pool settings
        hikariConfig.setMaximumPoolSize(config.getMaximumPoolSize());
        hikariConfig.setMinimumIdle(config.getMinimumIdle());
        hikariConfig.setConnectionTimeout(config.getConnectionTimeout());
        hikariConfig.setIdleTimeout(config.getIdleTimeout());
        hikariConfig.setMaxLifetime(config.getMaxLifetime());

        // MySQL optimizations
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
        hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
        hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
        hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
        hikariConfig.addDataSourceProperty("maintainTimeStats", "false");

        // Connection test
        hikariConfig.setConnectionTestQuery("SELECT 1");

        // Pool name
        hikariConfig.setPoolName("CasinoMySQL-Pool");

        return hikariConfig;
    }

    @Override
    protected void runMigrations() throws SQLException {
        logger.info("Running MySQL migrations...");

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Create migrations table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS casino_migrations (
                    version VARCHAR(50) PRIMARY KEY,
                    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);

            // Check if V1 already applied
            var rs = stmt.executeQuery("SELECT version FROM casino_migrations WHERE version = 'V1_InitialSchema'");
            if (rs.next()) {
                logger.info("Migration V1_InitialSchema already applied, skipping...");
                return;
            }

            // Run V1 migration
            V1_InitialSchema.applyMySQL(conn);

            // Record migration
            stmt.execute("INSERT INTO casino_migrations (version) VALUES ('V1_InitialSchema')");

            logger.info("MySQL migrations completed successfully!");
        }
    }

    @Override
    public String getDatabaseType() {
        return "mysql";
    }
}
