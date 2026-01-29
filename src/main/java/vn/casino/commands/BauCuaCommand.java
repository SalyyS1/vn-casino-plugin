package vn.casino.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import vn.casino.game.baucua.BauCuaGame;
import vn.casino.gui.framework.GuiManager;
import vn.casino.i18n.MessageKey;
import vn.casino.i18n.MessageManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Bau Cua game command handler.
 * Provides access to Bau Cua GUIs and features.
 *
 * Commands:
 * - /baucua - Open main GUI
 * - /baucua help - Show game rules
 * - /baucua history - Show transaction history
 * - /baucua top - Show leaderboard
 */
public class BauCuaCommand implements CommandExecutor, TabCompleter {

    private final GuiManager guiManager;
    private final BauCuaGame bauCuaGame;
    private final MessageManager messageManager;

    public BauCuaCommand(
        GuiManager guiManager,
        BauCuaGame bauCuaGame,
        MessageManager messageManager
    ) {
        this.guiManager = guiManager;
        this.bauCuaGame = bauCuaGame;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageManager.getRawMessage(MessageKey.GENERAL_PLAYER_ONLY));
            return true;
        }

        if (!player.hasPermission("casino.play")) {
            messageManager.sendMessage(player, MessageKey.GENERAL_NO_PERMISSION);
            return true;
        }

        if (args.length == 0) {
            // Open main Bau Cua GUI
            guiManager.openBauCua(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help" -> guiManager.openHelp(player, bauCuaGame);
            case "history" -> guiManager.openHistory(player);
            case "top" -> guiManager.openLeaderboard(player, bauCuaGame);
            default -> sendUsage(player);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String input = args[0].toLowerCase();

            for (String option : List.of("help", "history", "top")) {
                if (option.startsWith(input)) {
                    completions.add(option);
                }
            }

            return completions;
        }

        return List.of();
    }

    private void sendUsage(Player player) {
        player.sendMessage("§6§lBau Cua Commands:");
        player.sendMessage("§e/baucua §7- Open Bau Cua game");
        player.sendMessage("§e/baucua help §7- Show game rules");
        player.sendMessage("§e/baucua history §7- View your bet history");
        player.sendMessage("§e/baucua top §7- Show leaderboard");
    }
}
