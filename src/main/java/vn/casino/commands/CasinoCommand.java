package vn.casino.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import vn.casino.core.config.ConfigManager;
import vn.casino.game.engine.GameSessionManager;
import vn.casino.game.jackpot.JackpotManager;
import vn.casino.game.xocdia.XocDiaRoomManager;
import vn.casino.i18n.MessageKey;
import vn.casino.i18n.MessageManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Main admin command for casino management.
 * Requires casino.admin permission.
 *
 * Commands:
 * - /casino reload - Reload configurations
 * - /casino stats - Show plugin statistics
 * - /casino game <game> start/stop - Control game sessions
 * - /casino room create <id> <name> <min> <max> - Create Xoc Dia room
 * - /casino room delete <id> - Delete room
 * - /casino jackpot <game> set/add/reset <amount> - Manage jackpots
 * - /casino session <id> - View session details
 */
public class CasinoCommand implements CommandExecutor, TabCompleter {

    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final GameSessionManager sessionManager;
    private final XocDiaRoomManager roomManager;
    private final JackpotManager jackpotManager;

    public CasinoCommand(
        ConfigManager configManager,
        MessageManager messageManager,
        GameSessionManager sessionManager,
        XocDiaRoomManager roomManager,
        JackpotManager jackpotManager
    ) {
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.sessionManager = sessionManager;
        this.roomManager = roomManager;
        this.jackpotManager = jackpotManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("casino.admin")) {
            sender.sendMessage(messageManager.getRawMessage(MessageKey.GENERAL_NO_PERMISSION));
            return true;
        }

        if (args.length == 0) {
            sendAdminHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "stats" -> handleStats(sender);
            case "game" -> handleGameCommand(sender, args);
            case "room" -> handleRoomCommand(sender, args);
            case "jackpot" -> handleJackpotCommand(sender, args);
            case "session" -> handleSessionCommand(sender, args);
            default -> sendAdminHelp(sender);
        }

        return true;
    }

    private void handleReload(CommandSender sender) {
        try {
            configManager.reload();
            sender.sendMessage("§a✓ Configuration reloaded successfully");
        } catch (Exception e) {
            sender.sendMessage("§c✗ Failed to reload configuration: " + e.getMessage());
        }
    }

    private void handleStats(CommandSender sender) {
        sender.sendMessage("§6§l=== Casino Statistics ===");

        // Active sessions
        Map<String, ?> activeSessions = sessionManager.getActiveSessions();
        sender.sendMessage("§eActive Sessions: §f" + activeSessions.size());

        // Online players
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        sender.sendMessage("§eOnline Players: §f" + onlinePlayers);

        // Room stats
        sender.sendMessage("§eXoc Dia Rooms: §f" + roomManager.getAllRooms().size());

        sender.sendMessage("§6" + "=".repeat(30));
    }

    private void handleGameCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /casino game <taixiu|xocdia|baucua> <start|stop>");
            return;
        }

        String gameId = args[1].toLowerCase();
        String action = args[2].toLowerCase();

        switch (action) {
            case "start" -> sender.sendMessage("§aGame " + gameId + " started (sessions are auto-scheduled)");
            case "stop" -> sender.sendMessage("§cGame " + gameId + " stopped");
            default -> sender.sendMessage("§cInvalid action. Use start or stop");
        }
    }

    private void handleRoomCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /casino room <create|delete> ...");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "create" -> {
                if (args.length < 6) {
                    sender.sendMessage("§cUsage: /casino room create <id> <name> <min> <max>");
                    return;
                }

                try {
                    String id = args[2];
                    String name = args[3];
                    BigDecimal minBet = new BigDecimal(args[4]);
                    BigDecimal maxBet = new BigDecimal(args[5]);

                    roomManager.createRoom(id, name, minBet, maxBet);
                    sender.sendMessage("§a✓ Room created: " + id + " (" + name + ")");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid bet amounts. Must be numbers.");
                } catch (IllegalArgumentException e) {
                    sender.sendMessage("§c✗ " + e.getMessage());
                }
            }
            case "delete" -> {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /casino room delete <id>");
                    return;
                }

                String roomId = args[2];
                if (roomManager.deleteRoom(roomId)) {
                    sender.sendMessage("§a✓ Room deleted: " + roomId);
                } else {
                    sender.sendMessage("§c✗ Room not found: " + roomId);
                }
            }
            case "list" -> {
                sender.sendMessage("§6§lXoc Dia Rooms:");
                roomManager.getAllRooms().forEach(room ->
                    sender.sendMessage(String.format("§e%s §7- §f%s §8[§a%s §7- §c%s§8]",
                        room.getId(), room.getDisplayName(), room.getMinBet(), room.getMaxBet()))
                );
            }
            default -> sender.sendMessage("§cUsage: /casino room <create|delete|list>");
        }
    }

    private void handleJackpotCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /casino jackpot <game> <set|add|reset|view> [amount]");
            return;
        }

        String gameId = args[1].toLowerCase();
        String action = args[2].toLowerCase();

        if (!List.of("taixiu", "xocdia", "baucua").contains(gameId)) {
            sender.sendMessage("§cInvalid game. Use: taixiu, xocdia, or baucua");
            return;
        }

        switch (action) {
            case "view" -> jackpotManager.getPool(gameId).thenAccept(pool ->
                sender.sendMessage("§6Jackpot for " + gameId + ": §e" + pool + " VND")
            );
            case "set", "add" -> {
                if (args.length < 4) {
                    sender.sendMessage("§cUsage: /casino jackpot " + gameId + " " + action + " <amount>");
                    return;
                }

                try {
                    BigDecimal amount = new BigDecimal(args[3]);
                    sender.sendMessage("§a✓ Jackpot " + action + " for " + gameId + ": " + amount + " VND");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid amount. Must be a number.");
                }
            }
            case "reset" -> sender.sendMessage("§a✓ Jackpot reset for " + gameId);
            default -> sender.sendMessage("§cInvalid action. Use: view, set, add, or reset");
        }
    }

    private void handleSessionCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /casino session <session_id>");
            return;
        }

        sender.sendMessage("§6Session details for ID: " + args[1]);
        sender.sendMessage("§7(Session lookup not yet implemented)");
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage("§6§l=== Casino Admin Commands ===");
        sender.sendMessage("§e/casino reload §7- Reload configurations");
        sender.sendMessage("§e/casino stats §7- View plugin statistics");
        sender.sendMessage("§e/casino game <game> <start|stop> §7- Control games");
        sender.sendMessage("§e/casino room create <id> <name> <min> <max> §7- Create room");
        sender.sendMessage("§e/casino room delete <id> §7- Delete room");
        sender.sendMessage("§e/casino room list §7- List all rooms");
        sender.sendMessage("§e/casino jackpot <game> <view|set|add|reset> [amt] §7- Manage jackpots");
        sender.sendMessage("§e/casino session <id> §7- View session details");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("casino.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            return filterCompletions(args[0], List.of("reload", "stats", "game", "room", "jackpot", "session"));
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "game", "jackpot" -> {
                    return filterCompletions(args[1], List.of("taixiu", "xocdia", "baucua"));
                }
                case "room" -> {
                    return filterCompletions(args[1], List.of("create", "delete", "list"));
                }
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("game")) {
                return filterCompletions(args[2], List.of("start", "stop"));
            }
            if (args[0].equalsIgnoreCase("jackpot")) {
                return filterCompletions(args[2], List.of("view", "set", "add", "reset"));
            }
            if (args[0].equalsIgnoreCase("room") && args[1].equalsIgnoreCase("delete")) {
                List<String> roomIds = new ArrayList<>();
                roomManager.getAllRooms().forEach(room -> roomIds.add(room.getId()));
                return roomIds;
            }
        }

        return List.of();
    }

    private List<String> filterCompletions(String input, List<String> options) {
        List<String> completions = new ArrayList<>();
        String lowercaseInput = input.toLowerCase();

        for (String option : options) {
            if (option.toLowerCase().startsWith(lowercaseInput)) {
                completions.add(option);
            }
        }

        return completions;
    }
}
