package vn.casino.gui.framework;

import org.bukkit.entity.Player;
import vn.casino.CasinoPlugin;
import vn.casino.core.scheduler.FoliaScheduler;
import vn.casino.economy.TransactionRepository;
import vn.casino.game.baucua.BauCuaGame;
import vn.casino.game.engine.Game;
import vn.casino.game.engine.GameResult;
import vn.casino.game.taixiu.TaiXiuGame;
import vn.casino.game.xocdia.XocDiaGame;
import vn.casino.gui.common.BetAmountGui;
import vn.casino.gui.common.HelpGui;
import vn.casino.gui.common.HistoryGui;
import vn.casino.gui.common.LeaderboardGui;
import vn.casino.gui.games.*;
import vn.casino.i18n.MessageManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Central GUI manager and factory.
 * Responsibilities:
 * - Create and open all GUI types
 * - Track open GUIs per player
 * - Broadcast results to active GUIs
 * - Manage GUI lifecycle and cleanup
 * - Coordinate with GuiUpdater for countdowns
 */
public class GuiManager {

    private final CasinoPlugin plugin;
    private final FoliaScheduler scheduler;
    private final MessageManager messageManager;
    private final TransactionRepository transactionRepository;
    private final Map<UUID, BaseGameGui> openGuis;
    private final GuiUpdater updater;
    private final Logger logger;

    // Game instances (injected from plugin)
    private TaiXiuGame taiXiuGame;
    private XocDiaGame xocDiaGame;
    private BauCuaGame bauCuaGame;

    public GuiManager(
        CasinoPlugin plugin,
        FoliaScheduler scheduler,
        MessageManager messageManager,
        TransactionRepository transactionRepository
    ) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.messageManager = messageManager;
        this.transactionRepository = transactionRepository;
        this.openGuis = new ConcurrentHashMap<>();
        this.updater = new GuiUpdater(scheduler);
        this.logger = plugin.getLogger();
    }

    /**
     * Initialize with game instances.
     *
     * @param taiXiuGame Tai Xiu game
     * @param xocDiaGame Xoc Dia game
     * @param bauCuaGame Bau Cua game
     */
    public void setGames(TaiXiuGame taiXiuGame, XocDiaGame xocDiaGame, BauCuaGame bauCuaGame) {
        this.taiXiuGame = taiXiuGame;
        this.xocDiaGame = xocDiaGame;
        this.bauCuaGame = bauCuaGame;
    }

    // ========== Game GUI Openers ==========

    /**
     * Open Tai Xiu main GUI.
     */
    public void openTaiXiu(Player player) {
        closeIfOpen(player);
        TaiXiuMainGui gui = new TaiXiuMainGui(player, taiXiuGame, this, messageManager);
        openGuis.put(player.getUniqueId(), gui);
        scheduler.runAtEntity(player, () -> gui.show(player));
    }

    /**
     * Open Tai Xiu Soi Cau (pattern analysis).
     */
    public void openTaiXiuSoiCau(Player player) {
        closeIfOpen(player);
        TaiXiuSoiCauGui gui = new TaiXiuSoiCauGui(player, taiXiuGame, this, messageManager);
        scheduler.runAtEntity(player, () -> gui.show(player));
    }

    /**
     * Open Xoc Dia room selection.
     */
    public void openXocDiaRoomSelect(Player player) {
        closeIfOpen(player);
        XocDiaRoomSelectGui gui = new XocDiaRoomSelectGui(player, xocDiaGame, this, messageManager);
        scheduler.runAtEntity(player, () -> gui.show(player));
    }

    /**
     * Open Xoc Dia game GUI for specific room.
     *
     * @param roomId Room identifier
     */
    public void openXocDia(Player player, String roomId) {
        closeIfOpen(player);
        XocDiaGameGui gui = new XocDiaGameGui(player, xocDiaGame, roomId, this, messageManager);
        openGuis.put(player.getUniqueId(), gui);
        scheduler.runAtEntity(player, () -> gui.show(player));
    }

    /**
     * Open Bau Cua main GUI.
     */
    public void openBauCua(Player player) {
        closeIfOpen(player);
        BauCuaMainGui gui = new BauCuaMainGui(player, bauCuaGame, this, messageManager);
        openGuis.put(player.getUniqueId(), gui);
        scheduler.runAtEntity(player, () -> gui.show(player));
    }

    // ========== Common GUI Openers ==========

    /**
     * Open transaction history GUI.
     */
    public void openHistory(Player player) {
        HistoryGui gui = new HistoryGui(player, transactionRepository, this, scheduler, messageManager);
        scheduler.runAtEntity(player, () -> gui.show(player));
    }

    /**
     * Open leaderboard GUI.
     */
    public void openLeaderboard(Player player, Game game) {
        LeaderboardGui gui = new LeaderboardGui(player, game, this, scheduler, messageManager);
        scheduler.runAtEntity(player, () -> gui.show(player));
    }

    /**
     * Open bet amount selector GUI.
     *
     * @param gameGui Parent game GUI to return to
     */
    public void openBetAmount(Player player, BaseGameGui gameGui) {
        BetAmountGui gui = new BetAmountGui(player, gameGui, this, scheduler, messageManager);
        scheduler.runAtEntity(player, () -> gui.show(player));
    }

    /**
     * Open help/rules GUI.
     */
    public void openHelp(Player player, Game game) {
        HelpGui gui = new HelpGui(player, game, this, messageManager);
        scheduler.runAtEntity(player, () -> gui.show(player));
    }

    // ========== Broadcast & Lifecycle ==========

    /**
     * Broadcast game result to all open GUIs for that game.
     *
     * @param game Game that produced result
     * @param result Game result
     */
    public void broadcastResult(Game game, GameResult result) {
        for (Map.Entry<UUID, BaseGameGui> entry : openGuis.entrySet()) {
            BaseGameGui gui = entry.getValue();
            if (gui.getGame().getId().equals(game.getId())) {
                scheduler.runAtEntity(gui.getPlayer(), () -> {
                    gui.updateResultDisplay(result);
                });
            }
        }
    }

    /**
     * Close player's GUI if they have one open.
     */
    public void closeIfOpen(Player player) {
        BaseGameGui gui = openGuis.remove(player.getUniqueId());
        if (gui != null) {
            updater.stopCountdown(player.getUniqueId());
        }
    }

    /**
     * Unregister player's GUI (called from BaseGameGui.cleanup()).
     */
    public void unregister(Player player) {
        openGuis.remove(player.getUniqueId());
        updater.stopCountdown(player.getUniqueId());
    }

    /**
     * Get GUI updater instance.
     */
    public GuiUpdater getUpdater() {
        return updater;
    }

    /**
     * Get scheduler instance.
     */
    public FoliaScheduler getScheduler() {
        return scheduler;
    }

    /**
     * Shutdown all GUIs and cleanup.
     */
    public void shutdown() {
        openGuis.values().forEach(gui -> {
            Player player = gui.getPlayer();
            if (player.isOnline()) {
                player.closeInventory();
            }
        });
        openGuis.clear();
        updater.stopAll();
        logger.info("GuiManager shutdown complete");
    }
}
