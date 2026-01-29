package vn.casino.game.engine;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Bet record representing a single player bet in a game session.
 * Immutable record for bet tracking and payout calculation.
 *
 * @param id Unique bet ID (auto-generated from database)
 * @param sessionId Game session ID this bet belongs to
 * @param playerId Player UUID who placed the bet
 * @param betType Type of bet placed
 * @param amount Bet amount in VND
 * @param payout Payout amount if won (0 if lost)
 * @param won Whether this bet won
 * @param createdAt Timestamp when bet was placed
 */
public record Bet(
    Long id,
    Long sessionId,
    UUID playerId,
    BetType betType,
    BigDecimal amount,
    BigDecimal payout,
    boolean won,
    Instant createdAt
) {
    /**
     * Create a new bet (without ID for insertion).
     * Used when placing bets before database persistence.
     */
    public static Bet create(
        Long sessionId,
        UUID playerId,
        BetType betType,
        BigDecimal amount
    ) {
        return new Bet(
            null,
            sessionId,
            playerId,
            betType,
            amount,
            BigDecimal.ZERO,
            false,
            Instant.now()
        );
    }

    /**
     * Create winning bet with payout calculated.
     */
    public Bet withWin(BigDecimal payout) {
        return new Bet(
            id,
            sessionId,
            playerId,
            betType,
            amount,
            payout,
            true,
            createdAt
        );
    }

    /**
     * Create losing bet (payout = 0).
     */
    public Bet withLoss() {
        return new Bet(
            id,
            sessionId,
            playerId,
            betType,
            amount,
            BigDecimal.ZERO,
            false,
            createdAt
        );
    }

    /**
     * Validate bet invariants.
     */
    public Bet {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID cannot be null");
        }
        if (playerId == null) {
            throw new IllegalArgumentException("Player ID cannot be null");
        }
        if (betType == null) {
            throw new IllegalArgumentException("Bet type cannot be null");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Bet amount must be positive");
        }
        if (payout == null || payout.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Payout cannot be null or negative");
        }
    }
}
