package vn.casino.game.engine;

import java.util.Set;

/**
 * Game result record representing the outcome of a game session.
 * Contains provably fair data and calculated winning bet types.
 *
 * @param serverSeed Server seed used for RNG (revealed after result)
 * @param serverSeedHash SHA-256 hash of server seed (committed before betting)
 * @param rawValues Raw dice/disc values from RNG (e.g., [3, 4, 5] for Tai Xiu)
 * @param displayResult Human-readable result (e.g., "Tài (12)", "4 Đỏ", "Bầu-Cua-Tôm")
 * @param winningBets Set of bet types that won this round
 */
public record GameResult(
    String serverSeed,
    String serverSeedHash,
    int[] rawValues,
    String displayResult,
    Set<BetType> winningBets
) {
    /**
     * Validate result invariants.
     */
    public GameResult {
        if (serverSeed == null || serverSeed.isEmpty()) {
            throw new IllegalArgumentException("Server seed cannot be null or empty");
        }
        if (serverSeedHash == null || serverSeedHash.isEmpty()) {
            throw new IllegalArgumentException("Server seed hash cannot be null or empty");
        }
        if (rawValues == null || rawValues.length == 0) {
            throw new IllegalArgumentException("Raw values cannot be null or empty");
        }
        if (displayResult == null || displayResult.isEmpty()) {
            throw new IllegalArgumentException("Display result cannot be null or empty");
        }
        if (winningBets == null) {
            throw new IllegalArgumentException("Winning bets cannot be null (use empty set)");
        }
    }

    /**
     * Check if a specific bet type won.
     *
     * @param betType Bet type to check
     * @return true if this bet type won
     */
    public boolean isWinningBet(BetType betType) {
        return winningBets.contains(betType);
    }

    /**
     * Get total sum of raw values (useful for dice games like Tai Xiu).
     *
     * @return Sum of all raw values
     */
    public int getTotalValue() {
        int sum = 0;
        for (int value : rawValues) {
            sum += value;
        }
        return sum;
    }
}
