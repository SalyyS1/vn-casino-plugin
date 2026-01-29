package vn.casino.gui.games;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import vn.casino.game.xocdia.XocDiaGame;
import vn.casino.game.xocdia.XocDiaRoom;
import vn.casino.gui.framework.GuiButton;
import vn.casino.gui.framework.GuiManager;
import vn.casino.i18n.MessageManager;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Xoc Dia room selection GUI.
 * Shows available rooms with player counts and bet limits.
 */
public class XocDiaRoomSelectGui extends ChestGui {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getInstance(new Locale("vi", "VN"));

    private final Player player;
    private final XocDiaGame game;
    private final GuiManager guiManager;

    public XocDiaRoomSelectGui(
        Player player,
        XocDiaGame game,
        GuiManager guiManager,
        MessageManager messageManager
    ) {
        super(3, "Chọn Phòng Xóc Đĩa");
        this.player = player;
        this.game = game;
        this.guiManager = guiManager;

        setupLayout();
    }

    private void setupLayout() {
        StaticPane pane = new StaticPane(0, 0, 9, 3);

        List<XocDiaRoom> rooms = game.getRoomManager().getAllRooms();

        // Display room buttons
        int slot = 0;
        for (XocDiaRoom room : rooms) {
            if (slot >= 9) break; // Max 9 rooms in row 1

            Material material = getRoomMaterial(room);
            String formatted = createRoomDisplay(room);

            pane.addItem(GuiButton.create(
                material,
                "<yellow>" + room.getDisplayName() + "</yellow>",
                List.of(
                    "<gray>Người chơi: " + room.getPlayerCount() + "</gray>",
                    "<gray>Min: " + CURRENCY_FORMAT.format(room.getMinBet()) + " VND</gray>",
                    "<gray>Max: " + CURRENCY_FORMAT.format(room.getMaxBet()) + " VND</gray>",
                    "",
                    "<green>Click để vào phòng</green>"
                ),
                e -> {
                    game.getRoomManager().joinRoom(player.getUniqueId(), room.getId());
                    guiManager.openXocDia(player, room.getId());
                }
            ).toGuiItem(), slot % 9, slot / 9);

            slot++;
        }

        // Close button
        pane.addItem(GuiButton.create(
            Material.BARRIER,
            "<red>Đóng</red>",
            List.of("<gray>Đóng menu</gray>"),
            e -> player.closeInventory()
        ).toGuiItem(), 4, 2);

        addPane(pane);
    }

    /**
     * Get material based on room bet limits.
     */
    private Material getRoomMaterial(XocDiaRoom room) {
        long minBet = room.getMinBet().longValue();

        if (minBet >= 100000) {
            return Material.DIAMOND_BLOCK; // VIP room
        } else if (minBet >= 10000) {
            return Material.GOLD_BLOCK; // Medium room
        } else {
            return Material.IRON_BLOCK; // Beginner room
        }
    }

    /**
     * Create room display string.
     */
    private String createRoomDisplay(XocDiaRoom room) {
        return room.getDisplayName() + " (" + room.getPlayerCount() + " người)";
    }
}
