package vn.casino.gui.games;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import vn.casino.game.taixiu.TaiXiuGame;
import vn.casino.game.taixiu.TaiXiuResult;
import vn.casino.game.taixiu.TaiXiuSoiCau;
import vn.casino.gui.framework.GuiButton;
import vn.casino.gui.framework.GuiManager;
import vn.casino.i18n.MessageManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Tai Xiu Soi Cau (Pattern Analysis) GUI.
 * Displays:
 * - Last 20 results history
 * - Statistics (TAI/XIU percentages, streaks)
 * - Pattern predictions
 */
public class TaiXiuSoiCauGui extends ChestGui {

    private final Player player;
    private final TaiXiuGame game;
    private final TaiXiuSoiCau soiCau;
    private final GuiManager guiManager;

    public TaiXiuSoiCauGui(
        Player player,
        TaiXiuGame game,
        GuiManager guiManager,
        MessageManager messageManager
    ) {
        super(6, "Soi Cầu Tài Xỉu");
        this.player = player;
        this.game = game;
        this.soiCau = game.getSoiCau();
        this.guiManager = guiManager;

        setupLayout();
    }

    private void setupLayout() {
        StaticPane pane = new StaticPane(0, 0, 9, 6);

        // Statistics display (top row)
        setupStatistics(pane);

        // History display (rows 1-4)
        setupHistory(pane);

        // Back button
        pane.addItem(GuiButton.create(
            Material.BARRIER,
            "<red>Quay lại</red>",
            List.of("<gray>Trở về game</gray>"),
            e -> guiManager.openTaiXiu(player)
        ).toGuiItem(), 4, 5);

        addPane(pane);
    }

    /**
     * Setup statistics display.
     */
    private void setupStatistics(StaticPane pane) {
        TaiXiuSoiCau.Statistics stats = soiCau.getStatistics();

        // TAI percentage
        pane.addItem(GuiButton.createDisplay(
            Material.RED_STAINED_GLASS,
            "<red>TÀI: " + String.format("%.1f%%", stats.getTaiPercentage()) + "</red>",
            List.of(
                "<gray>Số lần: " + stats.getTaiCount() + "</gray>",
                "<gray>Streak: " + stats.getCurrentTaiStreak() + "</gray>",
                "<yellow>Max: " + stats.getLongestTaiStreak() + "</yellow>"
            )
        ).toGuiItem(), 1, 0);

        // XIU percentage
        pane.addItem(GuiButton.createDisplay(
            Material.BLUE_STAINED_GLASS,
            "<blue>XỈU: " + String.format("%.1f%%", stats.getXiuPercentage()) + "</blue>",
            List.of(
                "<gray>Số lần: " + stats.getXiuCount() + "</gray>",
                "<gray>Streak: " + stats.getCurrentXiuStreak() + "</gray>",
                "<yellow>Max: " + stats.getLongestXiuStreak() + "</yellow>"
            )
        ).toGuiItem(), 3, 0);

        // Triple percentage
        pane.addItem(GuiButton.createDisplay(
            Material.YELLOW_STAINED_GLASS,
            "<gold>TAM HOA: " + String.format("%.1f%%", stats.getTriplePercentage()) + "</gold>",
            List.of(
                "<gray>Số lần: " + stats.getTripleCount() + "</gray>"
            )
        ).toGuiItem(), 5, 0);

        // Total rounds
        pane.addItem(GuiButton.createDisplay(
            Material.BOOK,
            "<white>Tổng vòng: " + stats.getTotalRounds() + "</white>",
            List.of("<gray>Dữ liệu phân tích</gray>")
        ).toGuiItem(), 7, 0);
    }

    /**
     * Setup history display.
     */
    private void setupHistory(StaticPane pane) {
        List<TaiXiuResult> history = soiCau.getHistory();

        int slot = 0;
        for (int i = history.size() - 1; i >= 0 && slot < 36; i--) {
            TaiXiuResult result = history.get(i);
            int x = slot % 9;
            int y = 1 + (slot / 9);

            pane.addItem(createHistoryItem(result).toGuiItem(), x, y);
            slot++;
        }

        // Fill empty slots
        while (slot < 36) {
            int x = slot % 9;
            int y = 1 + (slot / 9);
            pane.addItem(GuiButton.createDisplay(
                Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                "<dark_gray>-</dark_gray>",
                List.of()
            ).toGuiItem(), x, y);
            slot++;
        }
    }

    /**
     * Create history item for a result.
     */
    private GuiButton createHistoryItem(TaiXiuResult result) {
        Material material;
        String color;
        String resultText;

        if (result.isTriple()) {
            material = Material.YELLOW_STAINED_GLASS;
            color = "<gold>";
            resultText = "TAM HOA";
        } else if (result.isTai()) {
            material = Material.RED_STAINED_GLASS;
            color = "<red>";
            resultText = "TÀI";
        } else {
            material = Material.BLUE_STAINED_GLASS;
            color = "<blue>";
            resultText = "XỈU";
        }

        return GuiButton.createDisplay(
            material,
            color + resultText + "</color>",
            List.of(
                "<gray>Xúc xắc: " + result.getDice1() + "-" + result.getDice2() + "-" + result.getDice3() + "</gray>",
                "<gray>Tổng: " + result.getTotal() + "</gray>"
            )
        );
    }
}
