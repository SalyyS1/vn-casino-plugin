package vn.casino.gui.common;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import vn.casino.game.engine.Game;
import vn.casino.gui.framework.GuiButton;
import vn.casino.gui.framework.GuiManager;
import vn.casino.i18n.MessageManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Help/Rules GUI for each game.
 * Displays game-specific rules and instructions.
 */
public class HelpGui extends ChestGui {

    private final Player player;
    private final Game game;
    private final GuiManager guiManager;

    public HelpGui(
        Player player,
        Game game,
        GuiManager guiManager,
        MessageManager messageManager
    ) {
        super(6, "Hướng Dẫn - " + game.getDisplayName());
        this.player = player;
        this.game = game;
        this.guiManager = guiManager;

        setupLayout();
    }

    private void setupLayout() {
        StaticPane pane = new StaticPane(0, 0, 9, 6);

        List<String> rules = getRulesForGame(game.getId());

        // Game icon
        Material icon = switch (game.getId()) {
            case "taixiu" -> Material.PAPER;
            case "xocdia" -> Material.RED_DYE;
            case "baucua" -> Material.TROPICAL_FISH;
            default -> Material.BOOK;
        };

        pane.addItem(GuiButton.createDisplay(
            icon,
            "<gold>" + game.getDisplayName() + "</gold>",
            rules
        ).toGuiItem(), 4, 1);

        // Min/Max bet info
        pane.addItem(GuiButton.createDisplay(
            Material.GOLD_INGOT,
            "<yellow>Giới hạn cược</yellow>",
            List.of(
                "<gray>Tối thiểu: " + game.getMinBet() + " VND</gray>",
                "<gray>Tối đa: " + game.getMaxBet() + " VND</gray>"
            )
        ).toGuiItem(), 2, 3);

        // Round duration info
        pane.addItem(GuiButton.createDisplay(
            Material.CLOCK,
            "<aqua>Thời gian vòng</aqua>",
            List.of(
                "<gray>Tổng: " + game.getRoundDuration().getSeconds() + "s</gray>",
                "<gray>Đặt cược: " + game.getBettingDuration().getSeconds() + "s</gray>"
            )
        ).toGuiItem(), 6, 3);

        // Close button
        pane.addItem(GuiButton.create(
            Material.BARRIER,
            "<red>Đóng</red>",
            List.of("<gray>Đóng hướng dẫn</gray>"),
            e -> player.closeInventory()
        ).toGuiItem(), 4, 5);

        addPane(pane);
    }

    /**
     * Get game-specific rules.
     */
    private List<String> getRulesForGame(String gameId) {
        return switch (gameId) {
            case "taixiu" -> List.of(
                "<yellow>Luật chơi Tài Xỉu:</yellow>",
                "",
                "<white>• 3 xúc xắc, mỗi con 1-6</white>",
                "<white>• Tổng từ 3-18</white>",
                "",
                "<green>TÀI (Lớn):</green>",
                "<gray>  Tổng >= 11 | Thắng x1.98</gray>",
                "",
                "<blue>XỈU (Nhỏ):</blue>",
                "<gray>  Tổng <= 10 | Thắng x1.98</gray>",
                "",
                "<red>Tam hoa (3 con giống nhau):</red>",
                "<gray>  Nhà cái thắng, tất cả thua</gray>"
            );
            case "xocdia" -> List.of(
                "<yellow>Luật chơi Xóc Đĩa:</yellow>",
                "",
                "<white>• 4 nút đỏ/trắng</white>",
                "<white>• 7 cửa cược</white>",
                "",
                "<red>CHẴN/LẺ:</red>",
                "<gray>  Số nút đỏ chẵn/lẻ | x1.96</gray>",
                "",
                "<gold>4 ĐỎ / 4 TRẮNG:</gold>",
                "<gray>  Cả 4 cùng màu | x11.0</gray>",
                "",
                "<aqua>3 ĐỎ / 3 TRẮNG:</aqua>",
                "<gray>  3 cùng màu | x3.2</gray>"
            );
            case "baucua" -> List.of(
                "<yellow>Luật chơi Bầu Cua:</yellow>",
                "",
                "<white>• 3 xúc xắc 6 mặt</white>",
                "<white>• 6 con vật: Bầu, Cua, Tôm, Cá, Gà, Nai</white>",
                "",
                "<green>Cược vào 1 con vật:</green>",
                "<gray>  1 con trùng: x2.0</gray>",
                "<gray>  2 con trùng: x3.0</gray>",
                "<gray>  3 con trùng: x4.0</gray>",
                "",
                "<yellow>Càng nhiều con trùng, thắng càng lớn!</yellow>"
            );
            default -> List.of("<gray>Không có hướng dẫn</gray>");
        };
    }
}
