package vn.casino.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import vn.casino.game.xocdia.XocDiaGame;
import vn.casino.game.xocdia.XocDiaRoomManager;
import vn.casino.gui.framework.GuiManager;
import vn.casino.i18n.MessageKey;
import vn.casino.i18n.MessageManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Xoc Dia game command handler.
 * Manages room-based Xoc Dia gameplay.
 *
 * Commands:
 * - /xocdia - Open room selection GUI
 * - /xocdia join <room> - Join specific room
 * - /xocdia leave - Leave current room
 * - /xocdia rooms - List available rooms
 * - /xocdia help - Show game rules
 */
public class XocDiaCommand implements CommandExecutor, TabCompleter {

    private final GuiManager guiManager;
    private final XocDiaGame xocDiaGame;
    private final XocDiaRoomManager roomManager;
    private final MessageManager messageManager;

    public XocDiaCommand(
        GuiManager guiManager,
        XocDiaGame xocDiaGame,
        XocDiaRoomManager roomManager,
        MessageManager messageManager
    ) {
        this.guiManager = guiManager;
        this.xocDiaGame = xocDiaGame;
        this.roomManager = roomManager;
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
            // Open room selection GUI
            guiManager.openXocDiaRoomSelect(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "join" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /xocdia join <room>");
                    return true;
                }
                String roomId = args[1];
                if (roomManager.getRoom(roomId) == null) {
                    player.sendMessage("§cRoom not found: " + roomId);
                    return true;
                }
                roomManager.joinRoom(player.getUniqueId(), roomId);
                guiManager.openXocDia(player, roomId);
                player.sendMessage("§aJoined room: §e" + roomId);
            }
            case "leave" -> {
                if (!roomManager.isPlayerInRoom(player.getUniqueId())) {
                    player.sendMessage("§cYou are not in any room");
                    return true;
                }
                String currentRoom = roomManager.getPlayerRoom(player.getUniqueId());
                roomManager.leaveRoom(player.getUniqueId());
                player.sendMessage("§aLeft room: §e" + currentRoom);
            }
            case "rooms" -> listRooms(player);
            case "help" -> guiManager.openHelp(player, xocDiaGame);
            default -> sendUsage(player);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String input = args[0].toLowerCase();

            for (String option : List.of("join", "leave", "rooms", "help")) {
                if (option.startsWith(input)) {
                    completions.add(option);
                }
            }

            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            // Tab complete room IDs
            List<String> roomIds = new ArrayList<>();
            roomManager.getAllRooms().forEach(room -> roomIds.add(room.getId()));
            return roomIds;
        }

        return List.of();
    }

    private void listRooms(Player player) {
        player.sendMessage("§6§lAvailable Xoc Dia Rooms:");
        roomManager.getAllRooms().forEach(room -> {
            int playerCount = room.getPlayers().size();
            player.sendMessage(String.format("§e%s §7- §f%s §8(§7%d players§8) §7[§a%s §7- §c%s§7]",
                room.getId(),
                room.getDisplayName(),
                playerCount,
                room.getMinBet(),
                room.getMaxBet()
            ));
        });
    }

    private void sendUsage(Player player) {
        player.sendMessage("§6§lXoc Dia Commands:");
        player.sendMessage("§e/xocdia §7- Open room selection");
        player.sendMessage("§e/xocdia join <room> §7- Join a room");
        player.sendMessage("§e/xocdia leave §7- Leave current room");
        player.sendMessage("§e/xocdia rooms §7- List available rooms");
        player.sendMessage("§e/xocdia help §7- Show game rules");
    }
}
