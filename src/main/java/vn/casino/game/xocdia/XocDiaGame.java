package vn.casino.game.xocdia;

import org.bukkit.entity.Player;
import vn.casino.economy.CurrencyManager;
import vn.casino.game.engine.*;
import vn.casino.game.jackpot.JackpotManager;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.logging.Logger;

/**
 * Xoc Dia (Shake the Plate) game implementation.
 * 4 discs (red/white), 7 bet types, multi-room system.
 */
public class XocDiaGame extends AbstractGame {
    private static final String GAME_ID = "xocdia";
    private final XocDiaConfig config;
    private final XocDiaRoomManager roomManager;

    /**
     * Create XocDia game with injected room manager.
     * @param roomManager Shared room manager instance (DO NOT create new one here)
     */
    public XocDiaGame(
        CurrencyManager currencyManager,
        JackpotManager jackpotManager,
        GameSessionManager sessionManager,
        Logger logger,
        XocDiaConfig config,
        XocDiaRoomManager roomManager
    ) {
        super(currencyManager, jackpotManager, sessionManager, logger);
        this.config = config;
        this.roomManager = roomManager;
    }

    @Override
    public String getId() {
        return GAME_ID;
    }

    @Override
    public String getDisplayName() {
        return "Xóc Đĩa";
    }

    @Override
    public Duration getRoundDuration() {
        return config.getRoundDuration();
    }

    @Override
    public Duration getBettingDuration() {
        return config.getBettingDuration();
    }

    @Override
    public BigDecimal getMinBet() {
        // Min bet varies by room, return lowest
        return roomManager.getAllRooms().stream()
            .map(XocDiaRoom::getMinBet)
            .min(BigDecimal::compareTo)
            .orElse(new BigDecimal("1000"));
    }

    @Override
    public BigDecimal getMaxBet() {
        // Max bet varies by room, return highest
        return roomManager.getAllRooms().stream()
            .map(XocDiaRoom::getMaxBet)
            .max(BigDecimal::compareTo)
            .orElse(new BigDecimal("100000000"));
    }

    @Override
    public List<XocDiaBetType> getAvailableBets() {
        return Arrays.asList(XocDiaBetType.values());
    }

    @Override
    public void onSessionStart(GameSession session) {
        logger.info("Xoc Dia session " + session.getId() + " started for room: " + session.getRoom());
    }

    @Override
    public boolean onBet(Player player, BetType betType, BigDecimal amount) {
        UUID playerId = player.getUniqueId();

        // Check if player is in a room
        String roomId = roomManager.getPlayerRoom(playerId);
        if (roomId == null) {
            player.sendMessage("§cYou must join a room first! Use /casino xocdia join <room>");
            return false;
        }

        // Get room and validate bet limits
        XocDiaRoom room = roomManager.getRoom(roomId);
        if (room == null) {
            player.sendMessage("§cRoom not found!");
            return false;
        }

        // Validate bet amount against room limits
        if (amount.compareTo(room.getMinBet()) < 0) {
            player.sendMessage("§cMinimum bet for this room is " + formatCurrency(room.getMinBet()));
            return false;
        }

        if (amount.compareTo(room.getMaxBet()) > 0) {
            player.sendMessage("§cMaximum bet for this room is " + formatCurrency(room.getMaxBet()));
            return false;
        }

        // Get session for this room
        GameSession session = room.getCurrentSession();
        if (session == null || session.getState() != GameSessionState.BETTING) {
            player.sendMessage("§cBetting is not currently active in this room!");
            return false;
        }

        // Use parent class bet validation and processing
        return super.onBet(player, betType, amount);
    }

    @Override
    public GameResult calculateResult(GameSession session) {
        String serverSeed = session.getServerSeed();
        long nonce = session.getId();

        // Generate 4 disc results (0 = white, 1 = red)
        boolean[] discs = new boolean[4];
        for (int i = 0; i < 4; i++) {
            int value = ProvablyFairRNG.generateResult(serverSeed, "disc", nonce + i, 2);
            discs[i] = (value == 1); // true = red
        }

        XocDiaResult xocDiaResult = new XocDiaResult(serverSeed, session.getServerSeedHash(), discs);
        return xocDiaResult.toGameResult();
    }

    @Override
    public GameSession getActiveSession(String room) {
        if (room == null) {
            logger.warning("Attempted to get active session without specifying room");
            return null;
        }
        return roomManager.getRoomSession(room);
    }

    /**
     * Get room manager for room operations.
     *
     * @return Room manager
     */
    public XocDiaRoomManager getRoomManager() {
        return roomManager;
    }

    /**
     * Get configuration.
     *
     * @return Game config
     */
    public XocDiaConfig getConfig() {
        return config;
    }

    /**
     * Get min bet for specific room.
     *
     * @param roomId Room ID
     * @return Min bet for room
     */
    public BigDecimal getMinBetForRoom(String roomId) {
        XocDiaRoom room = roomManager.getRoom(roomId);
        return room != null ? room.getMinBet() : getMinBet();
    }

    /**
     * Get max bet for specific room.
     *
     * @param roomId Room ID
     * @return Max bet for room
     */
    public BigDecimal getMaxBetForRoom(String roomId) {
        XocDiaRoom room = roomManager.getRoom(roomId);
        return room != null ? room.getMaxBet() : getMaxBet();
    }

    /**
     * Join player to a room.
     *
     * @param player Player to join
     * @param roomId Room ID
     * @return true if joined successfully
     */
    public boolean joinRoom(Player player, String roomId) {
        UUID playerId = player.getUniqueId();
        XocDiaRoom room = roomManager.getRoom(roomId);

        if (room == null) {
            player.sendMessage("§cRoom '" + roomId + "' does not exist!");
            return false;
        }

        // Check if player has minimum balance (10x min bet)
        try {
            boolean hasBalance = room.hasMinBalance(playerId, currencyManager).join();
            if (!hasBalance) {
                BigDecimal required = room.getMinBet().multiply(BigDecimal.TEN);
                player.sendMessage("§cYou need at least " + formatCurrency(required) + " to join this room!");
                return false;
            }
        } catch (Exception e) {
            logger.severe("Failed to check balance for player " + player.getName() + ": " + e.getMessage());
            player.sendMessage("§cFailed to check balance. Please try again.");
            return false;
        }

        // Join room
        roomManager.joinRoom(playerId, roomId);
        player.sendMessage("§aYou joined " + room.getDisplayName());
        player.sendMessage("§7Min bet: " + formatCurrency(room.getMinBet()) +
                         " | Max bet: " + formatCurrency(room.getMaxBet()));
        player.sendMessage("§7Players in room: " + room.getPlayerCount());

        logger.info("Player " + player.getName() + " joined Xoc Dia room: " + roomId);
        return true;
    }

    /**
     * Remove player from their current room.
     *
     * @param player Player to remove
     */
    public void leaveRoom(Player player) {
        UUID playerId = player.getUniqueId();
        String roomId = roomManager.getPlayerRoom(playerId);

        if (roomId == null) {
            player.sendMessage("§cYou are not in any room!");
            return;
        }

        roomManager.leaveRoom(playerId);
        player.sendMessage("§eYou left the room.");
        logger.info("Player " + player.getName() + " left Xoc Dia room: " + roomId);
    }

    /**
     * Clean up player data on disconnect.
     *
     * @param playerId Player UUID
     */
    @Override
    public void cleanupPlayer(UUID playerId) {
        super.cleanupPlayer(playerId);
        roomManager.cleanupPlayer(playerId);
    }
}
