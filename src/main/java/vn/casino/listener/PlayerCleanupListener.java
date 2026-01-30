package vn.casino.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import vn.casino.game.baucua.BauCuaGame;
import vn.casino.game.engine.AbstractGame;
import vn.casino.game.taixiu.TaiXiuGame;
import vn.casino.game.xocdia.XocDiaGame;
import vn.casino.gui.framework.GuiManager;

import java.util.UUID;

/**
 * Handles player cleanup on disconnect.
 * Cleans up GUI state and game-specific player data.
 */
public class PlayerCleanupListener implements Listener {

    private final GuiManager guiManager;
    private final TaiXiuGame taiXiuGame;
    private final XocDiaGame xocDiaGame;
    private final BauCuaGame bauCuaGame;

    public PlayerCleanupListener(
        GuiManager guiManager,
        TaiXiuGame taiXiuGame,
        XocDiaGame xocDiaGame,
        BauCuaGame bauCuaGame
    ) {
        this.guiManager = guiManager;
        this.taiXiuGame = taiXiuGame;
        this.xocDiaGame = xocDiaGame;
        this.bauCuaGame = bauCuaGame;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Close any open GUI
        guiManager.closeIfOpen(player);

        // Cleanup game-specific player state
        taiXiuGame.cleanupPlayer(playerId);
        xocDiaGame.cleanupPlayer(playerId);
        bauCuaGame.cleanupPlayer(playerId);
    }
}
