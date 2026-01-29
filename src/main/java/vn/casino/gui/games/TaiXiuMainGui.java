package vn.casino.gui.games;

import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import vn.casino.game.engine.GameResult;
import vn.casino.game.engine.GameSession;
import vn.casino.game.engine.GameSessionState;
import vn.casino.game.taixiu.TaiXiuBetType;
import vn.casino.game.taixiu.TaiXiuGame;
import vn.casino.game.taixiu.TaiXiuResult;
import vn.casino.gui.framework.BaseGameGui;
import vn.casino.gui.framework.GuiButton;
import vn.casino.gui.framework.GuiManager;
import vn.casino.i18n.MessageManager;

import java.util.List;

/**
 * Tai Xiu main betting GUI.
 * Layout:
 * - Row 0: Bet amount selector (center)
 * - Row 1-2: TAI buttons (left), XIU buttons (right)
 * - Row 3: Result dice display (center)
 * - Row 4: Soi Cau button
 * - Row 5: Common buttons (help, history, leaderboard)
 */
public class TaiXiuMainGui extends BaseGameGui {

    private final TaiXiuGame taiXiuGame;

    public TaiXiuMainGui(
        Player player,
        TaiXiuGame game,
        GuiManager guiManager,
        MessageManager messageManager
    ) {
        super(player, game, guiManager, messageManager, "Tài Xỉu", 6);
        this.taiXiuGame = game;
        setupLayout();
    }

    @Override
    protected void setupLayout() {
        StaticPane pane = new StaticPane(0, 0, 9, 6);

        // Bet amount selector (row 0, center)
        setupBetAmountButton(pane, 4, 0);

        // TAI buttons (left side - row 1-2, slots 0-1)
        setupTaiButton(pane, 0, 1);
        setupTaiButton(pane, 1, 1);
        setupTaiButton(pane, 0, 2);
        setupTaiButton(pane, 1, 2);

        // XIU buttons (right side - row 1-2, slots 7-8)
        setupXiuButton(pane, 7, 1);
        setupXiuButton(pane, 8, 1);
        setupXiuButton(pane, 7, 2);
        setupXiuButton(pane, 8, 2);

        // Result display (center - row 3)
        setupResultDisplay(pane);

        // Soi Cau button (row 4, center)
        pane.addItem(GuiButton.create(
            Material.COMPASS,
            "<light_purple>Soi Cầu</light_purple>",
            List.of(
                "<gray>Xem phân tích xu hướng</gray>",
                "<gray>và lịch sử kết quả</gray>"
            ),
            e -> guiManager.openTaiXiuSoiCau(player)
        ).toGuiItem(), 4, 4);

        // Common buttons (row 5)
        setupCommonButtons(pane);

        addPane(pane);
    }

    @Override
    protected void setupBetButtons(StaticPane pane) {
        // Implemented inline in setupLayout
    }

    /**
     * Setup TAI bet button.
     */
    private void setupTaiButton(StaticPane pane, int x, int y) {
        GameSession session = getCurrentSession(null);
        boolean canBet = session != null && session.getState() == GameSessionState.BETTING;

        List<String> lore = canBet
            ? List.of(
                "<gray>Tổng >= 11</gray>",
                "<yellow>Thắng x1.98</yellow>",
                "",
                "<green>Click để cược " + formatCurrency(selectedBetAmount) + " VND</green>"
            )
            : List.of(
                "<gray>Tổng >= 11</gray>",
                "<yellow>Thắng x1.98</yellow>",
                "",
                "<red>Chưa mở cược</red>"
            );

        pane.addItem(GuiButton.create(
            Material.RED_WOOL,
            "<red><bold>TÀI (Lớn)</bold></red>",
            lore,
            e -> {
                if (canBet) {
                    placeBet(TaiXiuBetType.TAI);
                } else {
                    player.sendMessage("§cChưa đến thời gian đặt cược!");
                }
            }
        ).toGuiItem(), x, y);
    }

    /**
     * Setup XIU bet button.
     */
    private void setupXiuButton(StaticPane pane, int x, int y) {
        GameSession session = getCurrentSession(null);
        boolean canBet = session != null && session.getState() == GameSessionState.BETTING;

        List<String> lore = canBet
            ? List.of(
                "<gray>Tổng <= 10</gray>",
                "<yellow>Thắng x1.98</yellow>",
                "",
                "<green>Click để cược " + formatCurrency(selectedBetAmount) + " VND</green>"
            )
            : List.of(
                "<gray>Tổng <= 10</gray>",
                "<yellow>Thắng x1.98</yellow>",
                "",
                "<red>Chưa mở cược</red>"
            );

        pane.addItem(GuiButton.create(
            Material.BLUE_WOOL,
            "<blue><bold>XỈU (Nhỏ)</bold></blue>",
            lore,
            e -> {
                if (canBet) {
                    placeBet(TaiXiuBetType.XIU);
                } else {
                    player.sendMessage("§cChưa đến thời gian đặt cược!");
                }
            }
        ).toGuiItem(), x, y);
    }

    /**
     * Setup result display (3 dice in center).
     */
    private void setupResultDisplay(StaticPane pane) {
        // Get last result from Soi Cau
        TaiXiuResult lastResult = taiXiuGame.getSoiCau().getLastResult();

        if (lastResult != null) {
            // Show last dice results
            pane.addItem(createDiceItem(lastResult.getDice1()).toGuiItem(), 3, 3);
            pane.addItem(createDiceItem(lastResult.getDice2()).toGuiItem(), 4, 3);
            pane.addItem(createDiceItem(lastResult.getDice3()).toGuiItem(), 5, 3);
        } else {
            // Show placeholder
            pane.addItem(GuiButton.createDisplay(
                Material.PAPER,
                "<gray>Chờ kết quả</gray>",
                List.of("<dark_gray>Chưa có vòng nào</dark_gray>")
            ).toGuiItem(), 4, 3);
        }
    }

    /**
     * Create dice display item.
     */
    private GuiButton createDiceItem(int value) {
        Material material = Material.PAPER;
        String color = "<white>";

        return GuiButton.createDisplay(
            material,
            color + "⚄ " + value + "</color>",
            List.of("<gray>Xúc xắc</gray>")
        );
    }

    /**
     * Place a bet.
     */
    private void placeBet(TaiXiuBetType betType) {
        boolean success = game.onBet(player, betType, selectedBetAmount);
        if (success) {
            player.sendMessage("§aCược thành công: §f" + formatCurrency(selectedBetAmount) +
                " VND §avào §f" + betType.getDisplayName());
        }
    }

    @Override
    public void updateResultDisplay(GameResult result) {
        // Broadcast result to player
        player.sendMessage("§e========== KẾT QUẢ TÀI XỈU ==========");
        player.sendMessage("§fKết quả: " + result.displayResult());
        player.sendMessage("§e====================================");

        // Refresh GUI to show new result
        guiManager.getScheduler().runAtEntity(player, this::refresh);
    }
}
