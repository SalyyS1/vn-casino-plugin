package vn.casino.gui.games;

import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import vn.casino.game.baucua.BauCuaBetType;
import vn.casino.game.baucua.BauCuaGame;
import vn.casino.game.baucua.BauCuaResult;
import vn.casino.game.engine.GameResult;
import vn.casino.game.engine.GameSession;
import vn.casino.game.engine.GameSessionState;
import vn.casino.gui.framework.BaseGameGui;
import vn.casino.gui.framework.GuiButton;
import vn.casino.gui.framework.GuiManager;
import vn.casino.i18n.MessageManager;

import java.util.List;

/**
 * Bau Cua main betting GUI.
 * Layout:
 * - Row 0: Bet amount selector (center)
 * - Row 1-2: 6 animal bet buttons (2 rows of 3)
 * - Row 3: Result display (3 dice)
 * - Row 4: Info display
 * - Row 5: Common buttons (help, history, leaderboard)
 */
public class BauCuaMainGui extends BaseGameGui {

    private final BauCuaGame bauCuaGame;

    public BauCuaMainGui(
        Player player,
        BauCuaGame game,
        GuiManager guiManager,
        MessageManager messageManager
    ) {
        super(player, game, guiManager, messageManager, "Bầu Cua", 6);
        this.bauCuaGame = game;
        setupLayout();
    }

    @Override
    protected void setupLayout() {
        StaticPane pane = new StaticPane(0, 0, 9, 6);

        // Bet amount selector (row 0, center)
        setupBetAmountButton(pane, 4, 0);

        // 6 animal buttons (rows 1-2)
        setupBetButtons(pane);

        // Result display (row 3, center - 3 dice)
        setupResultDisplay(pane);

        // Info display (row 4, center)
        setupInfoDisplay(pane);

        // Common buttons (row 5)
        setupCommonButtons(pane);

        addPane(pane);
    }

    @Override
    protected void setupBetButtons(StaticPane pane) {
        GameSession session = getCurrentSession(null);
        boolean canBet = session != null && session.getState() == GameSessionState.BETTING;

        // Row 1: BAU, CUA, TOM
        createAnimalButton(pane, BauCuaBetType.BAU, 2, 1, canBet);
        createAnimalButton(pane, BauCuaBetType.CUA, 4, 1, canBet);
        createAnimalButton(pane, BauCuaBetType.TOM, 6, 1, canBet);

        // Row 2: CA, NAI, GA
        createAnimalButton(pane, BauCuaBetType.CA, 2, 2, canBet);
        createAnimalButton(pane, BauCuaBetType.NAI, 4, 2, canBet);
        createAnimalButton(pane, BauCuaBetType.GA, 6, 2, canBet);
    }

    /**
     * Create bet button for an animal.
     */
    private void createAnimalButton(StaticPane pane, BauCuaBetType animal, int x, int y, boolean canBetDisplay) {
        List<String> lore = canBetDisplay
            ? List.of(
                "<gray>Cược vào " + animal.getDisplayName() + "</gray>",
                "",
                "<yellow>1 con: x2.0</yellow>",
                "<yellow>2 con: x3.0</yellow>",
                "<yellow>3 con: x4.0</yellow>",
                "",
                "<green>Click để cược " + formatCurrency(selectedBetAmount) + " VND</green>"
            )
            : List.of(
                "<gray>Cược vào " + animal.getDisplayName() + "</gray>",
                "",
                "<yellow>1 con: x2.0</yellow>",
                "<yellow>2 con: x3.0</yellow>",
                "<yellow>3 con: x4.0</yellow>",
                "",
                "<red>Chưa mở cược</red>"
            );

        String color = getAnimalColor(animal);

        pane.addItem(GuiButton.create(
            animal.getIcon(),
            color + animal.getDisplayName() + "</color>",
            lore,
            e -> {
                // Check state at click time, not construction time
                GameSession clickSession = getCurrentSession(null);
                boolean canBet = clickSession != null && clickSession.getState() == GameSessionState.BETTING;
                if (canBet) {
                    placeBet(animal);
                } else {
                    player.sendMessage("§cChưa đến thời gian đặt cược!");
                }
            }
        ).toGuiItem(), x, y);
    }

    /**
     * Setup result display (3 dice).
     */
    private void setupResultDisplay(StaticPane pane) {
        BauCuaResult lastResult = bauCuaGame.getLastResult();

        if (lastResult != null) {
            // Show 3 dice results
            pane.addItem(createDiceItem(lastResult.getDice1().getValue()).toGuiItem(), 3, 3);
            pane.addItem(createDiceItem(lastResult.getDice2().getValue()).toGuiItem(), 4, 3);
            pane.addItem(createDiceItem(lastResult.getDice3().getValue()).toGuiItem(), 5, 3);
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
     * Setup info display.
     */
    private void setupInfoDisplay(StaticPane pane) {
        pane.addItem(GuiButton.createDisplay(
            Material.BOOK,
            "<yellow>Luật chơi</yellow>",
            List.of(
                "<gray>3 xúc xắc, 6 con vật</gray>",
                "<gray>Thắng theo số con trùng:</gray>",
                "<white>1 con: x2.0</white>",
                "<white>2 con: x3.0</white>",
                "<white>3 con: x4.0</white>"
            )
        ).toGuiItem(), 4, 4);
    }

    /**
     * Create dice display item.
     */
    private GuiButton createDiceItem(int value) {
        BauCuaBetType animal = BauCuaBetType.fromDiceValue(value);
        String color = getAnimalColor(animal);

        return GuiButton.createDisplay(
            animal.getIcon(),
            color + animal.getDisplayName() + "</color>",
            List.of("<gray>Xúc xắc " + value + "</gray>")
        );
    }

    /**
     * Place a bet.
     */
    private void placeBet(BauCuaBetType animal) {
        boolean success = game.onBet(player, animal, selectedBetAmount);
        if (success) {
            player.sendMessage("§aCược thành công: §f" + formatCurrency(selectedBetAmount) +
                " VND §avào §f" + animal.getDisplayName());
        }
    }

    @Override
    public void updateResultDisplay(GameResult result) {
        // Reconstruct BauCuaResult from GameResult
        BauCuaResult bcResult = new BauCuaResult(
            result.serverSeed(),
            result.serverSeedHash(),
            result.rawValues()[0],
            result.rawValues()[1],
            result.rawValues()[2]
        );

        player.sendMessage("§e========== KẾT QUẢ BẦU CUA ==========");
        player.sendMessage("§fXúc xắc 1: §a" + bcResult.getDice1().getDisplayName());
        player.sendMessage("§fXúc xắc 2: §a" + bcResult.getDice2().getDisplayName());
        player.sendMessage("§fXúc xắc 3: §a" + bcResult.getDice3().getDisplayName());
        player.sendMessage("§e=====================================");

        guiManager.getScheduler().runAtEntity(player, this::refresh);
    }

    /**
     * Get color for animal.
     */
    private String getAnimalColor(BauCuaBetType animal) {
        return switch (animal) {
            case BAU -> "<green>";
            case CUA -> "<red>";
            case TOM -> "<light_purple>";
            case CA -> "<blue>";
            case NAI -> "<gold>";
            case GA -> "<yellow>";
        };
    }
}
