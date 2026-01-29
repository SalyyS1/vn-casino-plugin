package vn.casino.gui.framework;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import vn.casino.game.engine.Game;
import vn.casino.game.engine.GameResult;
import vn.casino.game.engine.GameSession;
import vn.casino.i18n.MessageManager;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Abstract base class for all game-specific GUIs.
 * Provides common functionality:
 * - Common button layout (help, history, leaderboard)
 * - Bet amount selector integration
 * - Result display updates
 * - Proper cleanup on close
 */
public abstract class BaseGameGui extends ChestGui {

    protected final Player player;
    protected final Game game;
    protected final GuiManager guiManager;
    protected final MessageManager messageManager;
    protected BigDecimal selectedBetAmount;

    protected static final NumberFormat CURRENCY_FORMAT = NumberFormat.getInstance(new Locale("vi", "VN"));

    /**
     * Create a new game GUI.
     *
     * @param player Player viewing the GUI
     * @param game Game instance
     * @param guiManager GUI manager
     * @param title GUI title
     * @param rows Number of rows (1-6)
     */
    protected BaseGameGui(
        Player player,
        Game game,
        GuiManager guiManager,
        MessageManager messageManager,
        String title,
        int rows
    ) {
        super(rows, title);
        this.player = player;
        this.game = game;
        this.guiManager = guiManager;
        this.messageManager = messageManager;
        this.selectedBetAmount = game.getMinBet();

        // Set close handler for cleanup
        setOnClose(event -> cleanup());
    }

    /**
     * Setup the GUI layout (called after construction and on refresh).
     */
    protected abstract void setupLayout();

    /**
     * Setup game-specific bet buttons.
     *
     * @param pane Pane to add buttons to
     */
    protected abstract void setupBetButtons(StaticPane pane);

    /**
     * Update result display when new result is broadcast.
     *
     * @param result Game result
     */
    public abstract void updateResultDisplay(GameResult result);

    /**
     * Setup common buttons (help, history, leaderboard).
     *
     * @param pane Pane to add buttons to
     */
    protected void setupCommonButtons(StaticPane pane) {
        // Help button (row 5, slot 4)
        pane.addItem(GuiButton.create(
            Material.BOOK,
            "<yellow>Hướng Dẫn</yellow>",
            List.of("<gray>Click để xem luật chơi</gray>"),
            e -> guiManager.openHelp(player, game)
        ).toGuiItem(), 4, 5);

        // History button (row 5, slot 7)
        pane.addItem(GuiButton.create(
            Material.CLOCK,
            "<aqua>Lịch Sử</aqua>",
            List.of("<gray>Click để xem lịch sử cược</gray>"),
            e -> guiManager.openHistory(player)
        ).toGuiItem(), 7, 5);

        // Leaderboard button (row 5, slot 1)
        pane.addItem(GuiButton.create(
            Material.GOLDEN_HELMET,
            "<gold>Bảng Xếp Hạng</gold>",
            List.of("<gray>Click để xem BXH</gray>"),
            e -> guiManager.openLeaderboard(player, game)
        ).toGuiItem(), 1, 5);
    }

    /**
     * Setup bet amount selector button.
     *
     * @param pane Pane to add button to
     * @param x X position (0-8)
     * @param y Y position (0-5)
     */
    protected void setupBetAmountButton(StaticPane pane, int x, int y) {
        String formatted = formatCurrency(selectedBetAmount);
        pane.addItem(GuiButton.create(
            Material.GOLD_INGOT,
            "<gold>Cược: " + formatted + " VND</gold>",
            List.of(
                "<gray>Click để thay đổi số tiền</gray>",
                "",
                "<yellow>Min: " + formatCurrency(game.getMinBet()) + "</yellow>",
                "<yellow>Max: " + formatCurrency(game.getMaxBet()) + "</yellow>"
            ),
            e -> guiManager.openBetAmount(player, this)
        ).toGuiItem(), x, y);
    }

    /**
     * Set the selected bet amount (called from BetAmountGui).
     *
     * @param amount New bet amount
     */
    public void setSelectedBetAmount(BigDecimal amount) {
        this.selectedBetAmount = amount;
        refresh();
    }

    /**
     * Get the selected bet amount.
     *
     * @return Current bet amount
     */
    public BigDecimal getSelectedBetAmount() {
        return selectedBetAmount;
    }

    /**
     * Get the game instance.
     *
     * @return Game
     */
    public Game getGame() {
        return game;
    }

    /**
     * Get the player viewing this GUI.
     *
     * @return Player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Refresh the GUI (rebuild layout and update display).
     */
    protected void refresh() {
        getPanes().clear();
        setupLayout();
        update();
    }

    /**
     * Cleanup resources when GUI is closed.
     */
    protected void cleanup() {
        guiManager.unregister(player);
    }

    /**
     * Format currency for display.
     *
     * @param amount Amount to format
     * @return Formatted string
     */
    protected String formatCurrency(BigDecimal amount) {
        return CURRENCY_FORMAT.format(amount);
    }

    /**
     * Get current game session.
     *
     * @param room Room ID (null for non-room games)
     * @return Active session or null
     */
    protected GameSession getCurrentSession(String room) {
        return game.getActiveSession(room);
    }
}
