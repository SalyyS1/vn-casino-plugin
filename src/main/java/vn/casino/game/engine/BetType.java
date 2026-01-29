package vn.casino.game.engine;

/**
 * Bet type interface for all game-specific bet types.
 * Each game implements this interface with their specific bet options.
 *
 * Examples:
 * - Tai Xiu: TAI (1.96x), XIU (1.96x)
 * - Xoc Dia: CHAN (1.96x), LE (1.96x), 4_DO (14.5x), 4_TRANG (14.5x)
 * - Bau Cua: BAU (5.88x), CUA (5.88x), etc.
 */
public interface BetType {
    /**
     * Get unique bet identifier (e.g., "tai", "4_do", "bau").
     *
     * @return Bet identifier
     */
    String getId();

    /**
     * Get display name for GUI (supports i18n keys).
     *
     * @return Display name or i18n key
     */
    String getDisplayName();

    /**
     * Get payout multiplier for this bet type.
     * Includes house edge (typically 2% deduction).
     *
     * @return Payout multiplier (e.g., 1.96 for even-money, 14.5 for rare bets)
     */
    double getPayoutMultiplier();
}
