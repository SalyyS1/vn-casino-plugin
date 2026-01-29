package vn.casino.game.engine;

import org.bukkit.entity.Player;
import vn.casino.economy.CurrencyManager;
import vn.casino.economy.TransactionType;
import vn.casino.game.jackpot.JackpotManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base class for all casino games.
 * Provides common functionality:
 * - Bet validation (min/max/balance)
 * - Bet cooldown (1 second between bets)
 * - Payout distribution
 * - Jackpot contribution (0.2% per bet)
 * - Session persistence
 */
public abstract class AbstractGame implements Game {

    protected final CurrencyManager currencyManager;
    protected final JackpotManager jackpotManager;
    protected final GameSessionManager sessionManager;
    protected final Logger logger;

    // Bet cooldown tracking (1 second between bets)
    private final Map<UUID, Instant> lastBetTime = new ConcurrentHashMap<>();
    private static final long BET_COOLDOWN_MS = 1000; // 1 second

    public AbstractGame(
        CurrencyManager currencyManager,
        JackpotManager jackpotManager,
        GameSessionManager sessionManager,
        Logger logger
    ) {
        this.currencyManager = currencyManager;
        this.jackpotManager = jackpotManager;
        this.sessionManager = sessionManager;
        this.logger = logger;
    }

    @Override
    public boolean onBet(Player player, BetType betType, BigDecimal amount) {
        UUID playerId = player.getUniqueId();
        GameSession session = getActiveSession(null);

        // Validate session state
        if (session == null || session.getState() != GameSessionState.BETTING) {
            player.sendMessage("§cBetting is not currently active!");
            return false;
        }

        // Check bet cooldown
        if (!checkBetCooldown(playerId)) {
            player.sendMessage("§cPlease wait before placing another bet!");
            return false;
        }

        // Validate bet amount
        if (amount.compareTo(getMinBet()) < 0) {
            player.sendMessage("§cMinimum bet is " + formatCurrency(getMinBet()));
            return false;
        }

        if (amount.compareTo(getMaxBet()) > 0) {
            player.sendMessage("§cMaximum bet is " + formatCurrency(getMaxBet()));
            return false;
        }

        // Check player balance
        CompletableFuture<Boolean> hasBalance = currencyManager.hasBalance(playerId, amount);
        try {
            if (!hasBalance.join()) {
                player.sendMessage("§cInsufficient balance!");
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to check balance for " + playerId, e);
            player.sendMessage("§cFailed to check balance. Please try again.");
            return false;
        }

        // Withdraw bet amount
        CompletableFuture<BigDecimal> withdrawal = currencyManager.withdraw(
            playerId,
            amount,
            TransactionType.BET,
            getId(),
            session.getId(),
            "Bet on " + betType.getDisplayName()
        );

        try {
            withdrawal.join();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to withdraw bet amount for " + playerId, e);
            player.sendMessage("§cFailed to place bet. Please try again.");
            return false;
        }

        // Create and add bet to session
        Bet bet = Bet.create(session.getId(), playerId, betType, amount);
        session.addBet(bet);

        // Contribute to jackpot (0.2% of bet)
        jackpotManager.contribute(getId(), amount);

        // Update bet cooldown
        lastBetTime.put(playerId, Instant.now());

        // Notify player
        player.sendMessage("§aBet placed: " + formatCurrency(amount) + " on " + betType.getDisplayName());

        logger.fine("Player " + player.getName() + " bet " + amount + " on " + betType.getDisplayName());

        return true;
    }

    @Override
    public Map<UUID, BigDecimal> calculatePayouts(GameSession session, GameResult result) {
        Map<UUID, BigDecimal> payouts = new HashMap<>();

        for (Bet bet : session.getAllBets()) {
            // Check if bet won
            if (result.isWinningBet(bet.betType())) {
                // Calculate payout
                BigDecimal payout = bet.amount()
                    .multiply(BigDecimal.valueOf(bet.betType().getPayoutMultiplier()));

                // Add to player's total payout
                payouts.merge(bet.playerId(), payout, BigDecimal::add);
            }
        }

        return payouts;
    }

    @Override
    public void onSessionEnd(GameSession session, GameResult result) {
        // Calculate and distribute payouts
        Map<UUID, BigDecimal> payouts = calculatePayouts(session, result);

        for (Map.Entry<UUID, BigDecimal> entry : payouts.entrySet()) {
            UUID playerId = entry.getKey();
            BigDecimal payout = entry.getValue();

            // Deposit winnings
            currencyManager.deposit(
                playerId,
                payout,
                TransactionType.WIN,
                getId(),
                session.getId(),
                "Win from " + session.getGameId()
            ).exceptionally(ex -> {
                logger.log(Level.SEVERE, "Failed to deposit winnings for " + playerId, ex);
                return null;
            });
        }

        // Check jackpot trigger
        checkJackpotTrigger(session);

        // Persist session to database
        sessionManager.persistSession(session, result);

        logger.info("Session " + session.getId() + " ended. Total bets: " +
            session.getAllBets().size() + ", Total payouts: " + payouts.size());
    }

    /**
     * Check if bet cooldown has passed.
     *
     * @param playerId Player UUID
     * @return true if cooldown passed
     */
    private boolean checkBetCooldown(UUID playerId) {
        Instant lastBet = lastBetTime.get(playerId);
        if (lastBet == null) {
            return true;
        }

        long elapsed = Instant.now().toEpochMilli() - lastBet.toEpochMilli();
        return elapsed >= BET_COOLDOWN_MS;
    }

    /**
     * Check and trigger jackpot if conditions met.
     *
     * @param session Completed session
     */
    private void checkJackpotTrigger(GameSession session) {
        if (session.getPlayers().isEmpty()) {
            return; // No players, no jackpot
        }

        jackpotManager.checkTrigger(getId(), session.getPlayers())
            .thenAccept(winner -> {
                if (winner.isPresent()) {
                    UUID winnerId = winner.get();
                    logger.info("Jackpot won by player " + winnerId + " in game " + getId());
                }
            })
            .exceptionally(ex -> {
                logger.log(Level.SEVERE, "Failed to check jackpot trigger", ex);
                return null;
            });
    }

    /**
     * Format currency for display.
     *
     * @param amount Amount to format
     * @return Formatted string
     */
    protected String formatCurrency(BigDecimal amount) {
        return String.format("%,.0f VND", amount);
    }

    /**
     * Clean up bet cooldown for player (on logout).
     *
     * @param playerId Player UUID
     */
    public void cleanupPlayer(UUID playerId) {
        lastBetTime.remove(playerId);
    }
}
