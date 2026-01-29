package vn.casino.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import vn.casino.game.taixiu.TaiXiuGame;
import vn.casino.gui.framework.GuiManager;
import vn.casino.i18n.MessageKey;
import vn.casino.i18n.MessageManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Tai Xiu game command handler.
 * Provides access to Tai Xiu GUIs and features.
 *
 * Commands:
 * - /taixiu - Open main GUI
 * - /taixiu help - Show game rules
 * - /taixiu history - Show transaction history
 * - /taixiu top - Show leaderboard
 * - /taixiu soicau - Show pattern analysis
 */
public class TaiXiuCommand implements CommandExecutor, TabCompleter {

    private final GuiManager guiManager;
    private final TaiXiuGame taiXiuGame;
    private final MessageManager messageManager;

    public TaiXiuCommand(
        GuiManager guiManager,
        TaiXiuGame taiXiuGame,
        MessageManager messageManager
    ) {
        this.guiManager = guiManager;
        this.taiXiuGame = taiXiuGame;
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
            // Open main Tai Xiu GUI
            guiManager.openTaiXiu(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help" -> guiManager.openHelp(player, taiXiuGame);
            case "history" -> guiManager.openHistory(player);
            case "top" -> guiManager.openLeaderboard(player, taiXiuGame);
            case "soicau" -> guiManager.openTaiXiuSoiCau(player);
            default -> sendUsage(player);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String input = args[0].toLowerCase();

            for (String option : List.of("help", "history", "top", "soicau")) {
                if (option.startsWith(input)) {
                    completions.add(option);
                }
            }

            return completions;
        }

        return List.of();
    }

    private void sendUsage(Player player) {
        player.sendMessage("§6§lTai Xiu Commands:");
        player.sendMessage("§e/taixiu §7- Open Tai Xiu game");
        player.sendMessage("§e/taixiu help §7- Show game rules");
        player.sendMessage("§e/taixiu history §7- View your bet history");
        player.sendMessage("§e/taixiu top §7- Show leaderboard");
        player.sendMessage("§e/taixiu soicau §7- Pattern analysis");
    }
}
