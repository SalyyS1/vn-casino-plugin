package vn.casino.gui.common;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import vn.casino.core.scheduler.FoliaScheduler;
import vn.casino.gui.framework.BaseGameGui;
import vn.casino.gui.framework.GuiButton;
import vn.casino.gui.framework.GuiManager;
import vn.casino.i18n.MessageManager;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Bet amount selector GUI with preset amounts and custom input.
 * Presets: 1k, 5k, 10k, 50k, 100k, 500k, 1M VND
 */
public class BetAmountGui extends ChestGui {

    private static final BigDecimal[] PRESETS = {
        new BigDecimal("1000"),
        new BigDecimal("5000"),
        new BigDecimal("10000"),
        new BigDecimal("50000"),
        new BigDecimal("100000"),
        new BigDecimal("500000"),
        new BigDecimal("1000000")
    };

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getInstance(new Locale("vi", "VN"));

    private final Player player;
    private final BaseGameGui parentGui;
    private final GuiManager guiManager;
    private final FoliaScheduler scheduler;

    public BetAmountGui(
        Player player,
        BaseGameGui parentGui,
        GuiManager guiManager,
        FoliaScheduler scheduler,
        MessageManager messageManager
    ) {
        super(3, "Chọn số tiền cược");
        this.player = player;
        this.parentGui = parentGui;
        this.guiManager = guiManager;
        this.scheduler = scheduler;

        setupLayout();
    }

    private void setupLayout() {
        StaticPane pane = new StaticPane(0, 0, 9, 3);

        BigDecimal minBet = parentGui.getGame().getMinBet();
        BigDecimal maxBet = parentGui.getGame().getMaxBet();

        // Add preset amount buttons
        int slot = 0;
        for (BigDecimal amount : PRESETS) {
            // Skip amounts outside min/max range
            if (amount.compareTo(minBet) < 0 || amount.compareTo(maxBet) > 0) {
                continue;
            }

            String formatted = CURRENCY_FORMAT.format(amount);
            Material material = getMaterialForAmount(amount);

            pane.addItem(GuiButton.create(
                material,
                "<yellow>" + formatted + " VND</yellow>",
                List.of(
                    "<gray>Click để chọn</gray>",
                    "",
                    "<green>✓ Trong giới hạn</green>"
                ),
                e -> {
                    parentGui.setSelectedBetAmount(amount);
                    scheduler.runAtEntity(player, () -> parentGui.show(player));
                }
            ).toGuiItem(), slot % 9, slot / 9);

            slot++;
        }

        // Back button
        pane.addItem(GuiButton.create(
            Material.BARRIER,
            "<red>Quay lại</red>",
            List.of("<gray>Trở về game</gray>"),
            e -> scheduler.runAtEntity(player, () -> parentGui.show(player))
        ).toGuiItem(), 8, 2);

        // Current selection display
        String currentFormatted = CURRENCY_FORMAT.format(parentGui.getSelectedBetAmount());
        pane.addItem(GuiButton.createDisplay(
            Material.GOLD_BLOCK,
            "<gold>Hiện tại: " + currentFormatted + "</gold>",
            List.of(
                "<gray>Số tiền đang chọn</gray>"
            )
        ).toGuiItem(), 4, 2);

        addPane(pane);
    }

    /**
     * Get appropriate material based on amount.
     */
    private Material getMaterialForAmount(BigDecimal amount) {
        if (amount.compareTo(new BigDecimal("100000")) >= 0) {
            return Material.GOLD_INGOT;
        } else if (amount.compareTo(new BigDecimal("10000")) >= 0) {
            return Material.GOLD_NUGGET;
        } else {
            return Material.COPPER_INGOT;
        }
    }
}
