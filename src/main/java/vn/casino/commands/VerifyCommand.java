package vn.casino.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vn.casino.core.database.DatabaseProvider;
import vn.casino.game.engine.ProvablyFairRNG;
import vn.casino.i18n.MessageKey;
import vn.casino.i18n.MessageManager;

import java.sql.ResultSet;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provably fair verification command.
 * Allows players to verify game session integrity.
 *
 * Command:
 * - /verify <session_id> - Verify game session
 */
public class VerifyCommand implements CommandExecutor {

    private final DatabaseProvider database;
    private final MessageManager messageManager;
    private final Logger logger;

    public VerifyCommand(
        DatabaseProvider database,
        MessageManager messageManager,
        Logger logger
    ) {
        this.database = database;
        this.messageManager = messageManager;
        this.logger = logger;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /verify <session_id>");
            sender.sendMessage("§7Example: /verify 1738172340567");
            return true;
        }

        long sessionId;
        try {
            sessionId = Long.parseLong(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c✗ Invalid session ID. Must be a number.");
            return true;
        }

        sender.sendMessage("§7Fetching session data...");

        // Load session from database
        loadSession(sessionId).thenAccept(sessionData -> {
            if (sessionData == null) {
                sender.sendMessage("§c✗ Session not found: " + sessionId);
                return;
            }

            // Verify hash
            boolean valid = ProvablyFairRNG.verify(
                sessionData.serverSeed,
                sessionData.serverSeedHash
            );

            // Display verification result
            sender.sendMessage("§6§l=== Session Verification ===");
            sender.sendMessage("§eSession ID: §f" + sessionId);
            sender.sendMessage("§eGame: §f" + sessionData.gameId);
            if (sessionData.room != null) {
                sender.sendMessage("§eRoom: §f" + sessionData.room);
            }
            sender.sendMessage("");
            sender.sendMessage("§eServer Seed: §7" + sessionData.serverSeed);
            sender.sendMessage("§eHash (SHA-256): §7" + sessionData.serverSeedHash);
            sender.sendMessage("");
            sender.sendMessage("§eResult: §f" + sessionData.resultDisplay);
            sender.sendMessage("§eRaw Values: §7" + sessionData.resultRawValues);
            sender.sendMessage("");

            if (valid) {
                sender.sendMessage("§a✓ VERIFIED - Hash matches server seed");
                sender.sendMessage("§aThis session is provably fair!");
            } else {
                sender.sendMessage("§c✗ INVALID - Hash does not match!");
                sender.sendMessage("§cThis session may have been tampered with!");
            }

            sender.sendMessage("§6" + "=".repeat(30));

            // Additional info for players
            if (sender instanceof Player) {
                sender.sendMessage("");
                sender.sendMessage("§7Provably fair means you can verify that");
                sender.sendMessage("§7the game result was generated fairly using");
                sender.sendMessage("§7cryptographic hashing (SHA-256).");
            }

        }).exceptionally(ex -> {
            sender.sendMessage("§c✗ Database error: " + ex.getMessage());
            logger.log(Level.SEVERE, "Failed to verify session " + sessionId, ex);
            return null;
        });

        return true;
    }

    /**
     * Load session data from database.
     *
     * @param sessionId Session ID to load
     * @return CompletableFuture with session data or null
     */
    private CompletableFuture<SessionData> loadSession(long sessionId) {
        String sql = """
            SELECT game_id, room, server_seed, server_seed_hash,
                   result_raw_values, result_display, state
            FROM casino_game_sessions
            WHERE id = ?
        """;

        return database.queryAsync(sql, rs -> {
            try {
                if (rs.next()) {
                    return new SessionData(
                        rs.getString("game_id"),
                        rs.getString("room"),
                        rs.getString("server_seed"),
                        rs.getString("server_seed_hash"),
                        rs.getString("result_raw_values"),
                        rs.getString("result_display"),
                        rs.getString("state")
                    );
                }
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to read session data", e);
            }
        }, sessionId);
    }

    /**
     * Session data holder.
     */
    private record SessionData(
        String gameId,
        String room,
        String serverSeed,
        String serverSeedHash,
        String resultRawValues,
        String resultDisplay,
        String state
    ) {}
}
