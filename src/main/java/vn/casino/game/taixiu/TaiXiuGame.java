package vn.casino.game.taixiu;

import org.bukkit.entity.Player;
import vn.casino.economy.CurrencyManager;
import vn.casino.game.engine.*;
import vn.casino.game.jackpot.JackpotManager;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Tai Xiu (Over/Under) dice game implementation.
 *
 * Game Rules:
 * - 3 dice, each rolling 1-6
 * - Total range: 3-18
 * - TAI (Big): total >= 11 (payout 1.98x)
 * - XIU (Small): total <= 10 (payout 1.98x)
 * - Triple (all 3 dice same): House wins, all bets lose
 *
 * Round Structure:
 * - 60 seconds total
 * - 50 seconds betting phase
 * - 10 seconds result display
 */
public class TaiXiuGame extends AbstractGame {

    private final TaiXiuConfig config;
    private final TaiXiuSoiCau soiCau;
    private GameSession currentSession;

    public TaiXiuGame(
        TaiXiuConfig config,
        CurrencyManager currencyManager,
        JackpotManager jackpotManager,
        GameSessionManager sessionManager,
        Logger logger
    ) {
        super(currencyManager, jackpotManager, sessionManager, logger);
        this.config = config;
        this.soiCau = new TaiXiuSoiCau(config.getHistoryDisplayCount());
    }

    @Override
    public String getId() {
        return "taixiu";
    }

    @Override
    public String getDisplayName() {
        return "Tài Xỉu";
    }

    @Override
    public Duration getRoundDuration() {
        return config.getRoundDuration();
    }

    @Override
    public Duration getBettingDuration() {
        return config.getBettingDuration();
    }

    @Override
    public BigDecimal getMinBet() {
        return config.getMinBet();
    }

    @Override
    public BigDecimal getMaxBet() {
        return config.getMaxBet();
    }

    @Override
    public List<? extends BetType> getAvailableBets() {
        return Arrays.asList(TaiXiuBetType.values());
    }

    @Override
    public void onSessionStart(GameSession session) {
        this.currentSession = session;
        logger.info("Tai Xiu session " + session.getId() + " started - Hash: " +
            session.getServerSeedHash().substring(0, 16) + "...");

        // Broadcast to players (would integrate with GUI/messaging system)
        // For now, just log
        logger.fine("Tai Xiu round started - Betting open for " +
            config.getBettingDuration().getSeconds() + " seconds");
    }

    @Override
    public GameResult calculateResult(GameSession session) {
        String serverSeed = session.getServerSeed();
        long nonce = session.getId();

        // Generate 3 dice rolls using provably fair RNG
        int dice1 = ProvablyFairRNG.rollDice(serverSeed, "dice1", nonce);
        int dice2 = ProvablyFairRNG.rollDice(serverSeed, "dice2", nonce + 1);
        int dice3 = ProvablyFairRNG.rollDice(serverSeed, "dice3", nonce + 2);

        // Create result
        TaiXiuResult result = new TaiXiuResult(
            serverSeed,
            session.getServerSeedHash(),
            dice1,
            dice2,
            dice3
        );

        // Add to history
        soiCau.addResult(result);

        logger.info("Tai Xiu result: " + result.toGameResult().displayResult() +
            " | Stats: " + soiCau.getStatsSummary());

        return result.toGameResult();
    }

    @Override
    public Map<UUID, BigDecimal> calculatePayouts(GameSession session, GameResult result) {
        // Use default payout calculation from AbstractGame
        // It checks result.isWinningBet() for each bet and applies multiplier
        return super.calculatePayouts(session, result);
    }

    @Override
    public void onSessionEnd(GameSession session, GameResult result) {
        // Call parent to handle payout distribution and jackpot
        super.onSessionEnd(session, result);

        // Reconstruct TaiXiuResult from GameResult's raw values
        TaiXiuResult txResult = new TaiXiuResult(
            result.serverSeed(),
            result.serverSeedHash(),
            result.rawValues()[0],
            result.rawValues()[1],
            result.rawValues()[2]
        );

        // Log session summary
        logger.info(String.format(
            "Tai Xiu session %d ended | Result: %s | Bets: %d | Total wagered: %s VND",
            session.getId(),
            result.displayResult(),
            session.getAllBets().size(),
            formatCurrency(session.getTotalBetAmount())
        ));

        // Clear current session reference
        this.currentSession = null;
    }

    @Override
    public GameSession getActiveSession(String room) {
        // Tai Xiu is not room-based, ignore room parameter
        return currentSession;
    }

    /**
     * Get Soi Cau (pattern tracker) for this game.
     *
     * @return Soi Cau instance
     */
    public TaiXiuSoiCau getSoiCau() {
        return soiCau;
    }

    /**
     * Get game configuration.
     *
     * @return Config instance
     */
    public TaiXiuConfig getConfig() {
        return config;
    }

    /**
     * Validate bet type for this game.
     *
     * @param betType Bet type to validate
     * @return true if valid for Tai Xiu
     */
    public boolean isValidBetType(BetType betType) {
        return betType instanceof TaiXiuBetType;
    }

    /**
     * Get current session state.
     *
     * @return Current state or null if no active session
     */
    public GameSessionState getCurrentState() {
        return currentSession != null ? currentSession.getState() : null;
    }

    /**
     * Check if betting is currently open.
     *
     * @return true if players can place bets
     */
    public boolean isBettingOpen() {
        return currentSession != null &&
               currentSession.getState() == GameSessionState.BETTING;
    }

    /**
     * Get remaining betting time in seconds.
     *
     * @return Seconds remaining or 0 if not betting
     */
    public long getRemainingBettingTime() {
        if (!isBettingOpen()) {
            return 0;
        }

        long elapsedSeconds = java.time.Duration.between(
            currentSession.getStartedAt(),
            java.time.Instant.now()
        ).getSeconds();

        long remainingSeconds = config.getBettingDuration().getSeconds() - elapsedSeconds;
        return Math.max(0, remainingSeconds);
    }
}
