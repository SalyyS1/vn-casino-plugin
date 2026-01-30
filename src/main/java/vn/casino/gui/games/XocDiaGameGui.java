package vn.casino.gui.games;

import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import vn.casino.game.engine.GameResult;
import vn.casino.game.engine.GameSession;
import vn.casino.game.engine.GameSessionState;
import vn.casino.game.xocdia.XocDiaBetType;
import vn.casino.game.xocdia.XocDiaGame;
import vn.casino.game.xocdia.XocDiaResult;
import vn.casino.gui.framework.BaseGameGui;
import vn.casino.gui.framework.GuiButton;
import vn.casino.gui.framework.GuiManager;
import vn.casino.i18n.MessageManager;

import java.util.List;

/**
 * Xoc Dia game GUI for a specific room.
 * Layout:
 * - Row 0: Room info and bet amount
 * - Row 1-2: 7 bet type buttons
 * - Row 3: Result display
 * - Row 4: Room switch button
 * - Row 5: Common buttons
 */
public class XocDiaGameGui extends BaseGameGui {

    private final XocDiaGame xocDiaGame;
    private final String roomId;

    public XocDiaGameGui(
        Player player,
        XocDiaGame game,
        String roomId,
        GuiManager guiManager,
        MessageManager messageManager
    ) {
        super(player, game, guiManager, messageManager, "Xóc Đĩa - " + roomId, 6);
        this.xocDiaGame = game;
        this.roomId = roomId;
        setupLayout();
    }

    @Override
    protected void setupLayout() {
        StaticPane pane = new StaticPane(0, 0, 9, 6);

        // Bet amount selector (row 0, center)
        setupBetAmountButton(pane, 4, 0);

        // Room info (row 0, left)
        pane.addItem(GuiButton.createDisplay(
            Material.OAK_SIGN,
            "<yellow>Phòng: " + roomId + "</yellow>",
            List.of(
                "<gray>Người chơi: " + getPlayerCount() + "</gray>"
            )
        ).toGuiItem(), 0, 0);

        // Setup 7 bet buttons (rows 1-2)
        setupBetButtons(pane);

        // Result display (row 3, center)
        setupResultDisplay(pane);

        // Room switch button (row 4, center)
        pane.addItem(GuiButton.create(
            Material.ENDER_PEARL,
            "<light_purple>Đổi Phòng</light_purple>",
            List.of("<gray>Click để chọn phòng khác</gray>"),
            e -> {
                xocDiaGame.getRoomManager().leaveRoom(player.getUniqueId());
                guiManager.openXocDiaRoomSelect(player);
            }
        ).toGuiItem(), 4, 4);

        // Common buttons (row 5)
        setupCommonButtons(pane);

        addPane(pane);
    }

    @Override
    protected void setupBetButtons(StaticPane pane) {
        GameSession session = getCurrentSession(roomId);
        boolean canBet = session != null && session.getState() == GameSessionState.BETTING;

        // Row 1: CHAN, LE, DO_4, TRANG_4
        createBetButton(pane, XocDiaBetType.CHAN, 1, 1, canBet);
        createBetButton(pane, XocDiaBetType.LE, 2, 1, canBet);
        createBetButton(pane, XocDiaBetType.DO_4, 3, 1, canBet);
        createBetButton(pane, XocDiaBetType.TRANG_4, 5, 1, canBet);

        // Row 2: 3-1 combinations
        createBetButton(pane, XocDiaBetType.DO_3_TRANG_1, 2, 2, canBet);
        createBetButton(pane, XocDiaBetType.DO_1_TRANG_3, 4, 2, canBet);
        createBetButton(pane, XocDiaBetType.DO_2_TRANG_2, 6, 2, canBet);
    }

    /**
     * Create bet button for specific bet type.
     */
    private void createBetButton(StaticPane pane, XocDiaBetType betType, int x, int y, boolean canBetDisplay) {
        Material material = getBetMaterial(betType);
        String color = getBetColor(betType);

        List<String> lore = canBetDisplay
            ? List.of(
                "<gray>" + getBetDescription(betType) + "</gray>",
                "<yellow>Thắng x" + betType.getPayoutMultiplier() + "</yellow>",
                "",
                "<green>Click để cược " + formatCurrency(selectedBetAmount) + " VND</green>"
            )
            : List.of(
                "<gray>" + getBetDescription(betType) + "</gray>",
                "<yellow>Thắng x" + betType.getPayoutMultiplier() + "</yellow>",
                "",
                "<red>Chưa mở cược</red>"
            );

        pane.addItem(GuiButton.create(
            material,
            color + betType.getDisplayName() + "</color>",
            lore,
            e -> {
                // Check state at click time, not construction time
                GameSession clickSession = getCurrentSession(roomId);
                boolean canBet = clickSession != null && clickSession.getState() == GameSessionState.BETTING;
                if (canBet) {
                    placeBet(betType);
                } else {
                    player.sendMessage("§cChưa đến thời gian đặt cược!");
                }
            }
        ).toGuiItem(), x, y);
    }

    /**
     * Setup result display.
     */
    private void setupResultDisplay(StaticPane pane) {
        // TODO: Get last result from game history
        pane.addItem(GuiButton.createDisplay(
            Material.RED_DYE,
            "<gray>Kết quả</gray>",
            List.of("<dark_gray>Chờ vòng tiếp theo</dark_gray>")
        ).toGuiItem(), 4, 3);
    }

    /**
     * Place a bet.
     */
    private void placeBet(XocDiaBetType betType) {
        boolean success = game.onBet(player, betType, selectedBetAmount);
        if (success) {
            player.sendMessage("§aCược thành công: §f" + formatCurrency(selectedBetAmount) +
                " VND §avào §f" + betType.getDisplayName());
        }
    }

    @Override
    public void updateResultDisplay(GameResult result) {
        player.sendMessage("§e========== KẾT QUẢ XÓC ĐĨA ==========");
        player.sendMessage("§fKết quả: " + result.displayResult());
        player.sendMessage("§e=====================================");

        guiManager.getScheduler().runAtEntity(player, this::refresh);
    }

    /**
     * Get material for bet type.
     */
    private Material getBetMaterial(XocDiaBetType betType) {
        return switch (betType) {
            case CHAN -> Material.WHITE_WOOL;
            case LE -> Material.RED_WOOL;
            case DO_4 -> Material.RED_CONCRETE;
            case TRANG_4 -> Material.WHITE_CONCRETE;
            case DO_3_TRANG_1 -> Material.RED_STAINED_GLASS;
            case DO_1_TRANG_3 -> Material.WHITE_STAINED_GLASS;
            case DO_2_TRANG_2 -> Material.PINK_WOOL;
        };
    }

    /**
     * Get color code for bet type.
     */
    private String getBetColor(XocDiaBetType betType) {
        return switch (betType) {
            case CHAN, TRANG_4, DO_1_TRANG_3 -> "<white>";
            case LE, DO_4, DO_3_TRANG_1 -> "<red>";
            case DO_2_TRANG_2 -> "<light_purple>";
        };
    }

    /**
     * Get description for bet type.
     */
    private String getBetDescription(XocDiaBetType betType) {
        return switch (betType) {
            case CHAN -> "0, 2, 4 đỏ";
            case LE -> "1, 3 đỏ";
            case DO_4 -> "4 đỏ";
            case TRANG_4 -> "4 trắng";
            case DO_3_TRANG_1 -> "3 đỏ, 1 trắng";
            case DO_1_TRANG_3 -> "1 đỏ, 3 trắng";
            case DO_2_TRANG_2 -> "2 đỏ, 2 trắng";
        };
    }

    /**
     * Get player count in current room.
     */
    private int getPlayerCount() {
        return xocDiaGame.getRoomManager().getRoom(roomId).getPlayerCount();
    }
}
