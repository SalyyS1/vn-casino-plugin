package vn.casino.game.xocdia;

import vn.casino.economy.CurrencyManager;
import vn.casino.game.engine.GameSession;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Room model for Xoc Dia game.
 * Each room has configurable min/max bets and tracks active players.
 */
public class XocDiaRoom {
    private final String id;
    private final String displayName;
    private final BigDecimal minBet;
    private final BigDecimal maxBet;
    private final Set<UUID> players;
    private GameSession currentSession;

    /**
     * Create a new Xoc Dia room.
     *
     * @param id Room identifier (e.g., "room1", "vip")
     * @param displayName Display name for GUI
     * @param minBet Minimum bet amount
     * @param maxBet Maximum bet amount
     */
    public XocDiaRoom(String id, String displayName, BigDecimal minBet, BigDecimal maxBet) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Room ID cannot be null or empty");
        }
        if (displayName == null || displayName.isEmpty()) {
            throw new IllegalArgumentException("Display name cannot be null or empty");
        }
        if (minBet == null || minBet.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Min bet must be positive");
        }
        if (maxBet == null || maxBet.compareTo(minBet) < 0) {
            throw new IllegalArgumentException("Max bet must be >= min bet");
        }

        this.id = id;
        this.displayName = displayName;
        this.minBet = minBet;
        this.maxBet = maxBet;
        this.players = ConcurrentHashMap.newKeySet();
        this.currentSession = null;
    }

    /**
     * Add player to room.
     *
     * @param playerId Player UUID
     */
    public void addPlayer(UUID playerId) {
        players.add(playerId);
    }

    /**
     * Remove player from room.
     *
     * @param playerId Player UUID
     */
    public void removePlayer(UUID playerId) {
        players.remove(playerId);
    }

    /**
     * Check if player is in this room.
     *
     * @param playerId Player UUID
     * @return true if player is in room
     */
    public boolean hasPlayer(UUID playerId) {
        return players.contains(playerId);
    }

    /**
     * Get current player count.
     *
     * @return Number of players in room
     */
    public int getPlayerCount() {
        return players.size();
    }

    /**
     * Get unmodifiable set of players.
     *
     * @return Set of player UUIDs
     */
    public Set<UUID> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    /**
     * Check if player has sufficient balance to join room.
     * Requires at least 10x min bet as buffer.
     *
     * @param playerId Player UUID
     * @param currencyManager Currency manager
     * @return CompletableFuture with true if player can afford
     */
    public CompletableFuture<Boolean> hasMinBalance(UUID playerId, CurrencyManager currencyManager) {
        BigDecimal requiredBalance = minBet.multiply(BigDecimal.TEN);
        return currencyManager.hasBalance(playerId, requiredBalance);
    }

    /**
     * Set current active session for this room.
     *
     * @param session Game session
     */
    public void setCurrentSession(GameSession session) {
        this.currentSession = session;
    }

    /**
     * Get current active session.
     *
     * @return Current session or null
     */
    public GameSession getCurrentSession() {
        return currentSession;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public BigDecimal getMinBet() {
        return minBet;
    }

    public BigDecimal getMaxBet() {
        return maxBet;
    }
}
