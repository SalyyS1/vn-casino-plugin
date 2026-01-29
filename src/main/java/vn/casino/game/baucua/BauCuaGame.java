package vn.casino.game.baucua;

import org.bukkit.entity.Player;
import vn.casino.economy.CurrencyManager;
import vn.casino.game.engine.*;
import vn.casino.game.jackpot.JackpotManager;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.logging.Logger;

/**
 * Bau Cua (Gourd-Crab-Shrimp-Fish-Deer-Rooster) game implementation.
 * Traditional Vietnamese dice game with 3 dice showing 6 animals.
 *
 * Game Rules:
 * - 3 dice, each showing one of 6 animals
 * - Players bet on which animals will appear
 * - Payout: 1:1 per matching die
 *   - 1 match = bet + (bet * 1) = 2x return
 *   - 2 matches = bet + (bet * 2) = 3x return
 *   - 3 matches (triple) = bet + (bet * 3) = 4x return
 * - 50 second rounds (40s betting, 10s result)
 *
 * Provably Fair:
 * - Uses SHA-256 based RNG with server seed commitment
 * - Players can verify results after round ends
 */
public class BauCuaGame extends AbstractGame {

    private final BauCuaConfig config;
    private GameSession activeSession;
    private BauCuaResult lastResult;

    /**
     * Create Bau Cua game instance.
     *
     * @param config Game configuration
     * @param currencyManager Currency manager for bets/payouts
     * @param jackpotManager Jackpot manager for progressive jackpot
     * @param sessionManager Session manager for persistence
     * @param logger Logger instance
     */
    public BauCuaGame(
        BauCuaConfig config,
        CurrencyManager currencyManager,
        JackpotManager jackpotManager,
        GameSessionManager sessionManager,
        Logger logger
    ) {
        super(currencyManager, jackpotManager, sessionManager, logger);
        this.config = config;
    }

    @Override
    public String getId() {
        return "baucua";
    }

    @Override
    public String getDisplayName() {
        return "Bầu Cua";
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
    public List<BauCuaBetType> getAvailableBets() {
        return Arrays.asList(BauCuaBetType.values());
    }

    @Override
    public void onSessionStart(GameSession session) {
        this.activeSession = session;
        session.setState(GameSessionState.BETTING);

        logger.info("Bau Cua session " + session.getId() + " started. Seed hash: " +
            session.getServerSeedHash());

        // Broadcast to all online players
        // TODO: Implement broadcast via event system or messaging
    }

    @Override
    public GameResult calculateResult(GameSession session) {
        String serverSeed = session.getServerSeed();
        String seedHash = session.getServerSeedHash();
        long sessionId = session.getId();

        // Generate 3 dice using provably fair RNG
        // Use session ID as nonce for deterministic results
        int dice1 = ProvablyFairRNG.rollDice(serverSeed, "baucua", sessionId);
        int dice2 = ProvablyFairRNG.rollDice(serverSeed, "baucua", sessionId + 1);
        int dice3 = ProvablyFairRNG.rollDice(serverSeed, "baucua", sessionId + 2);

        logger.info("Bau Cua session " + sessionId + " result: " +
            dice1 + "-" + dice2 + "-" + dice3);

        BauCuaResult result = new BauCuaResult(serverSeed, seedHash, dice1, dice2, dice3);
        this.lastResult = result;
        return result.toGameResult();
    }

    @Override
    public Map<UUID, BigDecimal> calculatePayouts(GameSession session, GameResult result) {
        // Convert GameResult back to BauCuaResult for match counting
        BauCuaResult bauCuaResult = new BauCuaResult(
            result.serverSeed(),
            result.serverSeedHash(),
            result.rawValues()[0],
            result.rawValues()[1],
            result.rawValues()[2]
        );

        Map<UUID, BigDecimal> payouts = new HashMap<>();

        // Calculate payouts for each player's bets
        for (Bet bet : session.getAllBets()) {
            BauCuaBetType animal = (BauCuaBetType) bet.betType();
            int matches = bauCuaResult.getMatchCount(animal);

            if (matches > 0) {
                // Payout = bet amount + (bet * matches * payoutPerMatch)
                // Example: 1000 VND bet, 2 matches = 1000 + (1000 * 2 * 1.0) = 3000 VND
                BigDecimal winAmount = bet.amount()
                    .multiply(BigDecimal.valueOf(matches))
                    .multiply(BigDecimal.valueOf(config.getPayoutPerMatch()));

                // Total payout = original bet + winnings
                BigDecimal totalPayout = bet.amount().add(winAmount);

                // Accumulate payouts per player
                payouts.merge(bet.playerId(), totalPayout, BigDecimal::add);
            }
        }

        logger.info("Bau Cua session " + session.getId() + " payouts: " +
            payouts.size() + " winners");

        return payouts;
    }

    @Override
    public void onSessionEnd(GameSession session, GameResult result) {
        // Call parent implementation for payout distribution and jackpot
        super.onSessionEnd(session, result);

        // Clear active session
        if (activeSession != null && activeSession.getId() == session.getId()) {
            activeSession = null;
        }

        logger.info("Bau Cua session " + session.getId() + " ended");
    }

    @Override
    public GameSession getActiveSession(String room) {
        // Bau Cua doesn't use rooms (room parameter ignored)
        return activeSession;
    }

    /**
     * Get game configuration.
     *
     * @return Config object
     */
    public BauCuaConfig getConfig() {
        return config;
    }

    /**
     * Get last game result.
     *
     * @return Last result or null if no rounds played
     */
    public BauCuaResult getLastResult() {
        return lastResult;
    }

    /**
     * Helper method to format result for display.
     *
     * @param result Game result
     * @return Formatted string (e.g., "Bầu - Cua - Tôm")
     */
    public String formatResult(GameResult result) {
        BauCuaResult bauCuaResult = new BauCuaResult(
            result.serverSeed(),
            result.serverSeedHash(),
            result.rawValues()[0],
            result.rawValues()[1],
            result.rawValues()[2]
        );
        return bauCuaResult.getDisplayResult();
    }

    /**
     * Check if a specific animal appears in the result.
     *
     * @param result Game result
     * @param animal Animal to check
     * @return Number of matches (0-3)
     */
    public int getMatchCount(GameResult result, BauCuaBetType animal) {
        BauCuaResult bauCuaResult = new BauCuaResult(
            result.serverSeed(),
            result.serverSeedHash(),
            result.rawValues()[0],
            result.rawValues()[1],
            result.rawValues()[2]
        );
        return bauCuaResult.getMatchCount(animal);
    }
}
