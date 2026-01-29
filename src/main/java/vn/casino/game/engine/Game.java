package vn.casino.game.engine;

import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Game interface for all casino games.
 * Defines the lifecycle and operations for each game type.
 *
 * Implementations:
 * - TaiXiuGame (Tai Xiu - 3 dice)
 * - XocDiaGame (Xoc Dia - 4 discs)
 * - BauCuaGame (Bau Cua - 3 dice with symbols)
 */
public interface Game {
    /**
     * Get unique game identifier.
     *
     * @return Game ID ("taixiu", "xocdia", "baucua")
     */
    String getId();

    /**
     * Get localized display name.
     *
     * @return Display name (can be i18n key)
     */
    String getDisplayName();

    /**
     * Get total round duration (betting + result display).
     *
     * @return Round duration
     */
    Duration getRoundDuration();

    /**
     * Get betting phase duration.
     *
     * @return Betting duration
     */
    Duration getBettingDuration();

    /**
     * Get minimum bet amount.
     *
     * @return Minimum bet
     */
    BigDecimal getMinBet();

    /**
     * Get maximum bet amount.
     *
     * @return Maximum bet
     */
    BigDecimal getMaxBet();

    /**
     * Get available bet types for this game.
     *
     * @return List of bet types
     */
    List<? extends BetType> getAvailableBets();

    /**
     * Called when a new session starts.
     * Initialize game-specific state.
     *
     * @param session New session
     */
    void onSessionStart(GameSession session);

    /**
     * Called when a player places a bet.
     * Validate and process bet placement.
     *
     * @param player Player placing bet
     * @param betType Type of bet
     * @param amount Bet amount
     * @return true if bet was accepted
     */
    boolean onBet(Player player, BetType betType, BigDecimal amount);

    /**
     * Calculate provably fair result for the session.
     * Uses server seed + client seeds to generate outcome.
     *
     * @param session Session to calculate result for
     * @return Game result
     */
    GameResult calculateResult(GameSession session);

    /**
     * Calculate payouts for all winning bets.
     *
     * @param session Game session
     * @param result Game result
     * @return Map of player UUID to total payout amount
     */
    Map<UUID, BigDecimal> calculatePayouts(GameSession session, GameResult result);

    /**
     * Called when session ends.
     * Cleanup and finalization.
     *
     * @param session Ended session
     * @param result Final result
     */
    void onSessionEnd(GameSession session, GameResult result);

    /**
     * Get current active session (null if none).
     * For room-based games, specify room.
     *
     * @param room Room identifier (null for non-room games)
     * @return Active session or null
     */
    GameSession getActiveSession(String room);
}
