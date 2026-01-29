package vn.casino.game.engine;

/**
 * Game session state lifecycle enum.
 * Represents the different phases a game session goes through.
 */
public enum GameSessionState {
    /**
     * Session created, waiting for round to start.
     */
    WAITING,

    /**
     * Round started, accepting bets from players.
     */
    BETTING,

    /**
     * Betting closed, generating provably fair result.
     */
    CALCULATING,

    /**
     * Result generated, displaying to players and distributing payouts.
     */
    RESULT,

    /**
     * Session complete, ready for cleanup.
     */
    ENDED
}
