package vn.casino.gui.common;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import vn.casino.core.scheduler.FoliaScheduler;
import vn.casino.game.engine.Game;
import vn.casino.gui.framework.GuiButton;
import vn.casino.gui.framework.GuiManager;
import vn.casino.i18n.MessageManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Leaderboard GUI showing top players.
 * Displays top 45 players with their stats.
 *
 * TODO: Integrate with LeaderboardManager when implemented in Phase 09
 */
public class LeaderboardGui extends ChestGui {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getInstance(new Locale("vi", "VN"));

    private final Player player;
    private final Game game;
    private final GuiManager guiManager;
    private final FoliaScheduler scheduler;

    public LeaderboardGui(
        Player player,
        Game game,
        GuiManager guiManager,
        FoliaScheduler scheduler,
        MessageManager messageManager
    ) {
        super(6, "Bảng Xếp Hạng - " + game.getDisplayName());
        this.player = player;
        this.game = game;
        this.guiManager = guiManager;
        this.scheduler = scheduler;

        setupLayout();
    }

    private void setupLayout() {
        StaticPane pane = new StaticPane(0, 0, 9, 6);

        // TODO: Load real leaderboard data from LeaderboardManager
        // For now, show placeholder message

        pane.addItem(GuiButton.createDisplay(
            Material.GOLDEN_HELMET,
            "<gold>Bảng Xếp Hạng</gold>",
            List.of(
                "<gray>Chức năng này sẽ được</gray>",
                "<gray>triển khai trong Phase 09</gray>",
                "",
                "<yellow>Sẽ hiển thị:</yellow>",
                "<white>• Top 45 người chơi</white>",
                "<white>• Tổng thắng/thua</white>",
                "<white>• Lợi nhuận ròng</white>",
                "<white>• Hạng tuần/tháng/tổng</white>"
            )
        ).toGuiItem(), 4, 2);

        // Close button
        pane.addItem(GuiButton.create(
            Material.BARRIER,
            "<red>Đóng</red>",
            List.of("<gray>Đóng bảng xếp hạng</gray>"),
            e -> player.closeInventory()
        ).toGuiItem(), 4, 5);

        addPane(pane);
    }

    /**
     * Create leaderboard entry item (for future implementation).
     */
    private void createLeaderboardEntry(StaticPane pane, int rank, String playerName,
                                       long totalWon, long totalLost, int x, int y) {
        Material material = switch (rank) {
            case 1 -> Material.GOLDEN_HELMET;
            case 2 -> Material.IRON_HELMET;
            case 3 -> Material.CHAINMAIL_HELMET;
            default -> Material.LEATHER_HELMET;
        };

        String rankColor = switch (rank) {
            case 1 -> "<gold>";
            case 2 -> "<gray>";
            case 3 -> "<#cd7f32>";
            default -> "<white>";
        };

        long netProfit = totalWon - totalLost;
        String profitColor = netProfit >= 0 ? "<green>" : "<red>";
        String profitSign = netProfit >= 0 ? "+" : "";

        pane.addItem(GuiButton.createDisplay(
            material,
            rankColor + "#" + rank + " " + playerName + "</color>",
            List.of(
                "<green>Thắng: " + CURRENCY_FORMAT.format(totalWon) + " VND</green>",
                "<red>Thua: " + CURRENCY_FORMAT.format(totalLost) + " VND</red>",
                profitColor + "Lợi nhuận: " + profitSign + CURRENCY_FORMAT.format(netProfit) + " VND</color>"
            )
        ).toGuiItem(), x, y);
    }
}
