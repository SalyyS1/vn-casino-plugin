package vn.casino.game.engine;

import vn.casino.core.database.DatabaseProvider;
import vn.casino.core.scheduler.FoliaScheduler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages game session lifecycle and scheduling.
 * Handles session creation, state transitions, and round scheduling.
 * Thread-safe for concurrent game operations.
 */
public class GameSessionManager {

    private final DatabaseProvider database;
    private final FoliaScheduler scheduler;
    private final Logger logger;

    // Active sessions: "gameId:room" -> GameSession (room is null for non-room games)
    private final Map<String, GameSession> activeSessions = new ConcurrentHashMap<>();

    // Session ID generator
    private final AtomicLong sessionIdCounter = new AtomicLong(System.currentTimeMillis());

    public GameSessionManager(
        DatabaseProvider database,
        FoliaScheduler scheduler,
        Logger logger
    ) {
        this.database = database;
        this.scheduler = scheduler;
        this.logger = logger;
    }

    /**
     * Create a new game session.
     *
     * @param game Game instance
     * @param room Room identifier (null for non-room games)
     * @return New session
     */
    public GameSession createSession(Game game, String room) {
        String sessionKey = getSessionKey(game.getId(), room);

        // Check if session already exists
        GameSession existing = activeSessions.get(sessionKey);
        if (existing != null && existing.getState() != GameSessionState.ENDED) {
            logger.warning("Session already exists for " + sessionKey);
            return existing;
        }

        // Generate server seed for provably fair RNG
        String serverSeed = ProvablyFairRNG.generateServerSeed();
        String serverSeedHash = ProvablyFairRNG.commitment(serverSeed);

        // Create new session
        long sessionId = sessionIdCounter.incrementAndGet();
        GameSession session = new GameSession(
            sessionId,
            game.getId(),
            room,
            serverSeed,
            serverSeedHash
        );

        // Store in active sessions
        activeSessions.put(sessionKey, session);

        // Persist to database
        persistSessionStart(session);

        logger.info("Created session " + sessionId + " for " + sessionKey +
            " (hash: " + serverSeedHash.substring(0, 8) + "...)");

        return session;
    }

    /**
     * Get active session for a game.
     *
     * @param gameId Game identifier
     * @param room Room identifier (null for non-room games)
     * @return Active session or null
     */
    public GameSession getSession(String gameId, String room) {
        String sessionKey = getSessionKey(gameId, room);
        return activeSessions.get(sessionKey);
    }

    /**
     * Start betting phase for a session.
     *
     * @param session Session to start betting
     */
    public void startBetting(GameSession session) {
        if (session.getState() != GameSessionState.WAITING) {
            logger.warning("Cannot start betting for session " + session.getId() +
                " - current state: " + session.getState());
            return;
        }

        session.setState(GameSessionState.BETTING);
        logger.info("Session " + session.getId() + " - Betting started");
    }

    /**
     * End betting phase for a session.
     *
     * @param session Session to end betting
     */
    public void endBetting(GameSession session) {
        if (session.getState() != GameSessionState.BETTING) {
            logger.warning("Cannot end betting for session " + session.getId() +
                " - current state: " + session.getState());
            return;
        }

        session.setState(GameSessionState.CALCULATING);
        logger.info("Session " + session.getId() + " - Betting ended (" +
            session.getAllBets().size() + " bets placed)");
    }

    /**
     * Calculate result for a session.
     *
     * @param session Session to calculate result
     * @param game Game instance
     */
    public void calculateResult(GameSession session, Game game) {
        if (session.getState() != GameSessionState.CALCULATING) {
            logger.warning("Cannot calculate result for session " + session.getId() +
                " - current state: " + session.getState());
            return;
        }

        try {
            GameResult result = game.calculateResult(session);
            session.setResult(result);
            session.setState(GameSessionState.RESULT);

            logger.info("Session " + session.getId() + " - Result: " + result.displayResult() +
                " (seed: " + result.serverSeed().substring(0, 8) + "...)");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to calculate result for session " + session.getId(), e);
            session.setState(GameSessionState.ENDED);
        }
    }

    /**
     * End session and cleanup.
     *
     * @param session Session to end
     * @param game Game instance
     */
    public void endSession(GameSession session, Game game) {
        if (session.getState() == GameSessionState.ENDED) {
            return; // Already ended
        }

        session.setState(GameSessionState.ENDED);

        // Trigger game-specific cleanup
        if (session.getResult() != null) {
            game.onSessionEnd(session, session.getResult());
        }

        // Remove from active sessions
        String sessionKey = getSessionKey(session.getGameId(), session.getRoom());
        activeSessions.remove(sessionKey);

        logger.info("Session " + session.getId() + " ended");
    }

    /**
     * Schedule automatic round progression for a game.
     *
     * @param game Game to schedule rounds for
     * @param room Room identifier (null for non-room games)
     */
    public void scheduleRound(Game game, String room) {
        Duration roundDuration = game.getRoundDuration();
        Duration bettingDuration = game.getBettingDuration();
        Duration resultDuration = roundDuration.minus(bettingDuration);

        // Create new session
        GameSession session = createSession(game, room);

        // Trigger session start
        game.onSessionStart(session);

        // Start betting immediately
        startBetting(session);

        // Schedule betting end
        scheduler.runLater(() -> {
            endBetting(session);
            calculateResult(session, game);
        }, bettingDuration.getSeconds() * 20); // Convert seconds to ticks

        // Schedule session end
        scheduler.runLater(() -> {
            endSession(session, game);
            // Schedule next round
            scheduleRound(game, room);
        }, roundDuration.getSeconds() * 20);

        logger.fine("Scheduled round for " + game.getId() +
            (room != null ? " (room: " + room + ")" : "") +
            " - betting: " + bettingDuration.getSeconds() + "s, result: " +
            resultDuration.getSeconds() + "s");
    }

    /**
     * Persist session start to database.
     *
     * @param session Session to persist
     */
    private void persistSessionStart(GameSession session) {
        String sql = """
            INSERT INTO casino_game_sessions
            (id, game_id, room, server_seed_hash, state, started_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        database.executeAsync(sql,
            session.getId(),
            session.getGameId(),
            session.getRoom(),
            session.getServerSeedHash(),
            session.getState().name(),
            session.getStartedAt()
        ).exceptionally(ex -> {
            logger.log(Level.SEVERE, "Failed to persist session start " + session.getId(), ex);
            return null;
        });
    }

    /**
     * Persist session completion and result to database.
     *
     * @param session Completed session
     * @param result Game result
     */
    public void persistSession(GameSession session, GameResult result) {
        String updateSessionSql = """
            UPDATE casino_game_sessions
            SET server_seed = ?, state = ?, result_raw_values = ?,
                result_display = ?, ended_at = ?
            WHERE id = ?
        """;

        String insertBetSql = """
            INSERT INTO casino_bets
            (session_id, player_uuid, bet_type_id, amount, payout, won, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = database.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Update session with result
                try (PreparedStatement stmt = conn.prepareStatement(updateSessionSql)) {
                    stmt.setString(1, result.serverSeed());
                    stmt.setString(2, session.getState().name());
                    stmt.setString(3, arrayToString(result.rawValues()));
                    stmt.setString(4, result.displayResult());
                    stmt.setObject(5, session.getEndedAt());
                    stmt.setLong(6, session.getId());
                    stmt.executeUpdate();
                }

                // Insert all bets
                try (PreparedStatement stmt = conn.prepareStatement(insertBetSql)) {
                    for (Bet bet : session.getAllBets()) {
                        boolean won = result.isWinningBet(bet.betType());
                        java.math.BigDecimal payout = won
                            ? bet.amount().multiply(java.math.BigDecimal.valueOf(bet.betType().getPayoutMultiplier()))
                            : java.math.BigDecimal.ZERO;

                        stmt.setLong(1, session.getId());
                        stmt.setString(2, bet.playerId().toString());
                        stmt.setString(3, bet.betType().getId());
                        stmt.setBigDecimal(4, bet.amount());
                        stmt.setBigDecimal(5, payout);
                        stmt.setBoolean(6, won);
                        stmt.setObject(7, bet.createdAt());
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }

                conn.commit();
                logger.fine("Persisted session " + session.getId() + " with " +
                    session.getAllBets().size() + " bets");

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to persist session " + session.getId(), e);
        }
    }

    /**
     * Get session key for map lookup.
     *
     * @param gameId Game identifier
     * @param room Room identifier (null for non-room games)
     * @return Session key
     */
    private String getSessionKey(String gameId, String room) {
        return room != null ? gameId + ":" + room : gameId;
    }

    /**
     * Convert int array to comma-separated string.
     *
     * @param values Array of integers
     * @return Comma-separated string
     */
    private String arrayToString(int[] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(values[i]);
        }
        return sb.toString();
    }

    /**
     * Get all active sessions (for monitoring/debugging).
     *
     * @return Map of active sessions
     */
    public Map<String, GameSession> getActiveSessions() {
        return new ConcurrentHashMap<>(activeSessions);
    }

    /**
     * Cancel all scheduled rounds (on shutdown).
     */
    public void shutdown() {
        // End all active sessions
        for (GameSession session : activeSessions.values()) {
            session.setState(GameSessionState.ENDED);
        }
        activeSessions.clear();
        logger.info("GameSessionManager shutdown - all sessions ended");
    }
}
