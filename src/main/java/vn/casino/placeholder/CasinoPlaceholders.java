package vn.casino.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import vn.casino.CasinoPlugin;
import vn.casino.economy.CurrencyManager;
import vn.casino.game.engine.GameSession;
import vn.casino.game.engine.GameSessionManager;
import vn.casino.game.jackpot.JackpotManager;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.logging.Level;

/**
 * PlaceholderAPI expansion for VN Casino.
 *
 * Available placeholders:
 * - %casino_balance% - Player VND balance
 * - %casino_balance_formatted% - Formatted balance with commas
 * - %casino_taixiu_session% - Current Tai Xiu session ID
 * - %casino_taixiu_countdown% - Seconds until Tai Xiu result
 * - %casino_taixiu_jackpot% - Tai Xiu jackpot pool
 * - %casino_xocdia_jackpot% - Xoc Dia jackpot pool
 * - %casino_baucua_jackpot% - Bau Cua jackpot pool
 * - %casino_wins_total% - Total player wins (placeholder for future)
 * - %casino_losses_total% - Total player losses (placeholder for future)
 * - %casino_profit_total% - Net profit (placeholder for future)
 * - %casino_rank_weekly% - Weekly leaderboard rank (placeholder for future)
 */
public class CasinoPlaceholders extends PlaceholderExpansion {

    private final CasinoPlugin plugin;
    private final CurrencyManager currencyManager;
    private final GameSessionManager sessionManager;
    private final JackpotManager jackpotManager;
    private final NumberFormat numberFormat;

    public CasinoPlaceholders(
        CasinoPlugin plugin,
        CurrencyManager currencyManager,
        GameSessionManager sessionManager,
        JackpotManager jackpotManager
    ) {
        this.plugin = plugin;
        this.currencyManager = currencyManager;
        this.sessionManager = sessionManager;
        this.jackpotManager = jackpotManager;
        this.numberFormat = NumberFormat.getInstance(Locale.US);
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "casino";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "SalyVn";
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // Keep expansion loaded even if plugin reloads
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        // Non-player specific placeholders
        switch (params.toLowerCase()) {
            case "taixiu_session":
                return getTaiXiuSessionId();

            case "taixiu_countdown":
                return getTaiXiuCountdown();

            case "taixiu_jackpot":
                return getJackpot("taixiu");

            case "xocdia_jackpot":
                return getJackpot("xocdia");

            case "baucua_jackpot":
                return getJackpot("baucua");
        }

        // Player-specific placeholders (require player to be online or in cache)
        if (player == null) {
            return "N/A";
        }

        return switch (params.toLowerCase()) {
            case "balance" -> getBalance(player);
            case "balance_formatted" -> getBalanceFormatted(player);
            case "wins_total" -> "0"; // Placeholder for future stats implementation
            case "losses_total" -> "0"; // Placeholder for future stats implementation
            case "profit_total" -> "0"; // Placeholder for future stats implementation
            case "rank_weekly" -> "N/A"; // Placeholder for future leaderboard implementation
            default -> null; // Unknown placeholder
        };
    }

    /**
     * Get player's VND balance.
     */
    private String getBalance(OfflinePlayer player) {
        try {
            // Try to get from cache synchronously (if available)
            BigDecimal balance = currencyManager.getBalance(player.getUniqueId())
                .get(100, java.util.concurrent.TimeUnit.MILLISECONDS);
            return balance.toPlainString();
        } catch (Exception e) {
            // Fallback on timeout or error
            plugin.getLogger().log(Level.FINE, "Failed to get balance for placeholder: " + e.getMessage());
            return "0";
        }
    }

    /**
     * Get player's formatted balance with commas.
     */
    private String getBalanceFormatted(OfflinePlayer player) {
        try {
            BigDecimal balance = currencyManager.getBalance(player.getUniqueId())
                .get(100, java.util.concurrent.TimeUnit.MILLISECONDS);
            return numberFormat.format(balance);
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE, "Failed to get formatted balance: " + e.getMessage());
            return "0";
        }
    }

    /**
     * Get current Tai Xiu session ID.
     */
    private String getTaiXiuSessionId() {
        try {
            GameSession session = sessionManager.getSession("taixiu", null);
            return session != null ? String.valueOf(session.getId()) : "N/A";
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE, "Failed to get Tai Xiu session: " + e.getMessage());
            return "N/A";
        }
    }

    /**
     * Get Tai Xiu countdown in seconds.
     */
    private String getTaiXiuCountdown() {
        try {
            GameSession session = sessionManager.getSession("taixiu", null);
            if (session == null) {
                return "0";
            }

            // Calculate remaining time based on session start time
            Instant startTime = session.getStartedAt();
            if (startTime == null) {
                return "0";
            }

            // Assume 60 second rounds (this should be configurable)
            Duration roundDuration = Duration.ofSeconds(60);
            Duration elapsed = Duration.between(startTime, Instant.now());
            Duration remaining = roundDuration.minus(elapsed);

            long seconds = Math.max(0, remaining.getSeconds());
            return String.valueOf(seconds);
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE, "Failed to get Tai Xiu countdown: " + e.getMessage());
            return "0";
        }
    }

    /**
     * Get jackpot pool for a game.
     */
    private String getJackpot(String gameId) {
        try {
            BigDecimal jackpot = jackpotManager.getPool(gameId)
                .get(100, java.util.concurrent.TimeUnit.MILLISECONDS);
            return numberFormat.format(jackpot);
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE, "Failed to get jackpot for " + gameId + ": " + e.getMessage());
            return "0";
        }
    }
}
