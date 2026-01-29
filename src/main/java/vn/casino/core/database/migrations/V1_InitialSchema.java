package vn.casino.core.database.migrations;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Initial database schema migration.
 * Creates all required tables for the casino plugin.
 */
public class V1_InitialSchema {

    /**
     * Apply migration for MySQL/MariaDB.
     */
    public static void applyMySQL(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Casino players table - stores player balances
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS casino_players (
                    uuid CHAR(36) PRIMARY KEY,
                    balance DECIMAL(20, 2) NOT NULL DEFAULT 0.00,
                    total_wagered DECIMAL(20, 2) NOT NULL DEFAULT 0.00,
                    total_won DECIMAL(20, 2) NOT NULL DEFAULT 0.00,
                    total_lost DECIMAL(20, 2) NOT NULL DEFAULT 0.00,
                    games_played INT NOT NULL DEFAULT 0,
                    vip_tier VARCHAR(20) DEFAULT NULL,
                    last_played TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_balance (balance),
                    INDEX idx_vip_tier (vip_tier),
                    INDEX idx_last_played (last_played)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);

            // Casino transactions table - stores all monetary transactions
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS casino_transactions (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    uuid CHAR(36) NOT NULL,
                    type ENUM('DEPOSIT', 'WITHDRAW', 'BET', 'WIN', 'REFUND', 'JACKPOT') NOT NULL,
                    amount DECIMAL(20, 2) NOT NULL,
                    balance_before DECIMAL(20, 2) NOT NULL,
                    balance_after DECIMAL(20, 2) NOT NULL,
                    game VARCHAR(50) DEFAULT NULL,
                    session_id BIGINT DEFAULT NULL,
                    description TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_uuid (uuid),
                    INDEX idx_type (type),
                    INDEX idx_game (game),
                    INDEX idx_session_id (session_id),
                    INDEX idx_created_at (created_at),
                    FOREIGN KEY (uuid) REFERENCES casino_players(uuid) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);

            // Casino game sessions table - stores game session metadata
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS casino_game_sessions (
                    id BIGINT PRIMARY KEY,
                    game_id VARCHAR(50) NOT NULL,
                    room VARCHAR(100) DEFAULT NULL,
                    server_seed VARCHAR(128) DEFAULT NULL,
                    server_seed_hash VARCHAR(64) NOT NULL,
                    state ENUM('WAITING', 'BETTING', 'CALCULATING', 'RESULT', 'ENDED') NOT NULL DEFAULT 'WAITING',
                    result_raw_values VARCHAR(255) DEFAULT NULL,
                    result_display VARCHAR(255) DEFAULT NULL,
                    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    ended_at TIMESTAMP NULL DEFAULT NULL,
                    INDEX idx_game_id (game_id),
                    INDEX idx_room (room),
                    INDEX idx_state (state),
                    INDEX idx_started_at (started_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);

            // Casino bets table - stores individual bets
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS casino_bets (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    session_id BIGINT NOT NULL,
                    player_uuid CHAR(36) NOT NULL,
                    bet_type_id VARCHAR(50) NOT NULL,
                    amount DECIMAL(20, 2) NOT NULL,
                    payout DECIMAL(20, 2) NOT NULL DEFAULT 0.00,
                    won BOOLEAN NOT NULL DEFAULT FALSE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_session_id (session_id),
                    INDEX idx_player_uuid (player_uuid),
                    INDEX idx_won (won),
                    INDEX idx_created_at (created_at),
                    FOREIGN KEY (session_id) REFERENCES casino_game_sessions(id) ON DELETE CASCADE,
                    FOREIGN KEY (player_uuid) REFERENCES casino_players(uuid) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);

            // Casino jackpots table - stores progressive jackpot pools
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS casino_jackpots (
                    game_id VARCHAR(50) PRIMARY KEY,
                    pool_amount DECIMAL(20, 2) NOT NULL DEFAULT 0.00,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_pool_amount (pool_amount)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);

            // Casino jackpot wins table - stores jackpot win history
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS casino_jackpot_wins (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    game_id VARCHAR(50) NOT NULL,
                    winner_uuid CHAR(36) NOT NULL,
                    amount DECIMAL(20, 2) NOT NULL,
                    won_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_game_id (game_id),
                    INDEX idx_winner_uuid (winner_uuid),
                    INDEX idx_won_at (won_at),
                    FOREIGN KEY (winner_uuid) REFERENCES casino_players(uuid) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);

            // Casino leaderboard table - stores leaderboard data
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS casino_leaderboard (
                    uuid CHAR(36) NOT NULL,
                    period VARCHAR(20) NOT NULL,
                    game VARCHAR(50) NOT NULL,
                    total_wagered DECIMAL(20, 2) NOT NULL DEFAULT 0.00,
                    total_won DECIMAL(20, 2) NOT NULL DEFAULT 0.00,
                    net_profit DECIMAL(20, 2) NOT NULL DEFAULT 0.00,
                    games_played INT NOT NULL DEFAULT 0,
                    biggest_win DECIMAL(20, 2) NOT NULL DEFAULT 0.00,
                    win_rate DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
                    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (uuid, period, game),
                    INDEX idx_period_game (period, game),
                    INDEX idx_net_profit (net_profit),
                    INDEX idx_total_wagered (total_wagered),
                    INDEX idx_biggest_win (biggest_win),
                    FOREIGN KEY (uuid) REFERENCES casino_players(uuid) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);

            // Insert default jackpot entries for each game
            stmt.execute("""
                INSERT IGNORE INTO casino_jackpots (game_id, pool_amount)
                VALUES
                    ('taixiu', 10000.00),
                    ('xocdia', 10000.00),
                    ('baucua', 10000.00)
            """);
        }
    }

    /**
     * Apply migration for SQLite.
     * SQLite doesn't support ENUM types, so we use TEXT with CHECK constraints.
     */
    public static void applySQLite(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Casino players table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS casino_players (
                    uuid TEXT PRIMARY KEY,
                    balance REAL NOT NULL DEFAULT 0.00,
                    total_wagered REAL NOT NULL DEFAULT 0.00,
                    total_won REAL NOT NULL DEFAULT 0.00,
                    total_lost REAL NOT NULL DEFAULT 0.00,
                    games_played INTEGER NOT NULL DEFAULT 0,
                    vip_tier TEXT DEFAULT NULL,
                    last_played INTEGER DEFAULT (strftime('%s', 'now')),
                    created_at INTEGER DEFAULT (strftime('%s', 'now')),
                    updated_at INTEGER DEFAULT (strftime('%s', 'now'))
                )
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_players_balance ON casino_players(balance)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_players_vip_tier ON casino_players(vip_tier)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_players_last_played ON casino_players(last_played)");

            // Casino transactions table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS casino_transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uuid TEXT NOT NULL,
                    type TEXT NOT NULL CHECK(type IN ('DEPOSIT', 'WITHDRAW', 'BET', 'WIN', 'REFUND', 'JACKPOT')),
                    amount REAL NOT NULL,
                    balance_before REAL NOT NULL,
                    balance_after REAL NOT NULL,
                    game TEXT DEFAULT NULL,
                    session_id INTEGER DEFAULT NULL,
                    description TEXT,
                    created_at INTEGER DEFAULT (strftime('%s', 'now')),
                    FOREIGN KEY (uuid) REFERENCES casino_players(uuid) ON DELETE CASCADE
                )
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_transactions_uuid ON casino_transactions(uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_transactions_type ON casino_transactions(type)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_transactions_game ON casino_transactions(game)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_transactions_session_id ON casino_transactions(session_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON casino_transactions(created_at)");

            // Casino game sessions table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS casino_game_sessions (
                    id INTEGER PRIMARY KEY,
                    game_id TEXT NOT NULL,
                    room TEXT DEFAULT NULL,
                    server_seed TEXT DEFAULT NULL,
                    server_seed_hash TEXT NOT NULL,
                    state TEXT NOT NULL DEFAULT 'WAITING' CHECK(state IN ('WAITING', 'BETTING', 'CALCULATING', 'RESULT', 'ENDED')),
                    result_raw_values TEXT DEFAULT NULL,
                    result_display TEXT DEFAULT NULL,
                    started_at INTEGER DEFAULT (strftime('%s', 'now')),
                    ended_at INTEGER DEFAULT NULL
                )
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_game_sessions_game_id ON casino_game_sessions(game_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_game_sessions_room ON casino_game_sessions(room)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_game_sessions_state ON casino_game_sessions(state)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_game_sessions_started_at ON casino_game_sessions(started_at)");

            // Casino bets table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS casino_bets (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    session_id INTEGER NOT NULL,
                    player_uuid TEXT NOT NULL,
                    bet_type_id TEXT NOT NULL,
                    amount REAL NOT NULL,
                    payout REAL NOT NULL DEFAULT 0.00,
                    won INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER DEFAULT (strftime('%s', 'now')),
                    FOREIGN KEY (session_id) REFERENCES casino_game_sessions(id) ON DELETE CASCADE,
                    FOREIGN KEY (player_uuid) REFERENCES casino_players(uuid) ON DELETE CASCADE
                )
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_bets_session_id ON casino_bets(session_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_bets_player_uuid ON casino_bets(player_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_bets_won ON casino_bets(won)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_bets_created_at ON casino_bets(created_at)");

            // Casino jackpots table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS casino_jackpots (
                    game_id TEXT PRIMARY KEY,
                    pool_amount REAL NOT NULL DEFAULT 0.00,
                    updated_at INTEGER DEFAULT (strftime('%s', 'now'))
                )
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_jackpots_pool_amount ON casino_jackpots(pool_amount)");

            // Casino jackpot wins table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS casino_jackpot_wins (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    game_id TEXT NOT NULL,
                    winner_uuid TEXT NOT NULL,
                    amount REAL NOT NULL,
                    won_at INTEGER DEFAULT (strftime('%s', 'now')),
                    FOREIGN KEY (winner_uuid) REFERENCES casino_players(uuid) ON DELETE CASCADE
                )
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_jackpot_wins_game_id ON casino_jackpot_wins(game_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_jackpot_wins_winner_uuid ON casino_jackpot_wins(winner_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_jackpot_wins_won_at ON casino_jackpot_wins(won_at)");

            // Casino leaderboard table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS casino_leaderboard (
                    uuid TEXT NOT NULL,
                    period TEXT NOT NULL,
                    game TEXT NOT NULL,
                    total_wagered REAL NOT NULL DEFAULT 0.00,
                    total_won REAL NOT NULL DEFAULT 0.00,
                    net_profit REAL NOT NULL DEFAULT 0.00,
                    games_played INTEGER NOT NULL DEFAULT 0,
                    biggest_win REAL NOT NULL DEFAULT 0.00,
                    win_rate REAL NOT NULL DEFAULT 0.00,
                    last_updated INTEGER DEFAULT (strftime('%s', 'now')),
                    PRIMARY KEY (uuid, period, game),
                    FOREIGN KEY (uuid) REFERENCES casino_players(uuid) ON DELETE CASCADE
                )
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_leaderboard_period_game ON casino_leaderboard(period, game)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_leaderboard_net_profit ON casino_leaderboard(net_profit)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_leaderboard_total_wagered ON casino_leaderboard(total_wagered)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_leaderboard_biggest_win ON casino_leaderboard(biggest_win)");

            // Insert default jackpot entries
            stmt.execute("""
                INSERT OR IGNORE INTO casino_jackpots (game_id, pool_amount)
                VALUES
                    ('taixiu', 10000.00),
                    ('xocdia', 10000.00),
                    ('baucua', 10000.00)
            """);
        }
    }
}
