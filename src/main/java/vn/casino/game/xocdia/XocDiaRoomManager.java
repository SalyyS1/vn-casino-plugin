package vn.casino.game.xocdia;

import vn.casino.game.engine.GameSession;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Room manager for Xoc Dia game.
 * Manages multiple rooms with different bet limits and player tracking.
 */
public class XocDiaRoomManager {
    private final Map<String, XocDiaRoom> rooms;
    private final Map<UUID, String> playerRooms; // Track which room each player is in

    /**
     * Create room manager with initial rooms from config.
     *
     * @param config Xoc Dia configuration
     */
    public XocDiaRoomManager(XocDiaConfig config) {
        this.rooms = new ConcurrentHashMap<>();
        this.playerRooms = new ConcurrentHashMap<>();

        // Load rooms from config
        for (XocDiaConfig.RoomConfig roomConfig : config.getRooms()) {
            XocDiaRoom room = new XocDiaRoom(
                roomConfig.id(),
                roomConfig.displayName(),
                roomConfig.minBet(),
                roomConfig.maxBet()
            );
            rooms.put(roomConfig.id(), room);
        }
    }

    /**
     * Get room by ID.
     *
     * @param roomId Room identifier
     * @return Room or null if not found
     */
    public XocDiaRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    /**
     * Get all available rooms.
     *
     * @return List of all rooms
     */
    public List<XocDiaRoom> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }

    /**
     * Create a new room.
     *
     * @param id Room ID
     * @param displayName Display name
     * @param minBet Minimum bet
     * @param maxBet Maximum bet
     * @return Created room
     * @throws IllegalArgumentException if room ID already exists
     */
    public XocDiaRoom createRoom(String id, String displayName, BigDecimal minBet, BigDecimal maxBet) {
        if (rooms.containsKey(id)) {
            throw new IllegalArgumentException("Room with ID '" + id + "' already exists");
        }

        XocDiaRoom room = new XocDiaRoom(id, displayName, minBet, maxBet);
        rooms.put(id, room);
        return room;
    }

    /**
     * Delete a room.
     * Kicks all players from the room first.
     *
     * @param roomId Room ID to delete
     * @return true if room was deleted
     */
    public boolean deleteRoom(String roomId) {
        XocDiaRoom room = rooms.get(roomId);
        if (room == null) {
            return false;
        }

        // Remove all players from room
        for (UUID playerId : new HashSet<>(room.getPlayers())) {
            leaveRoom(playerId);
        }

        rooms.remove(roomId);
        return true;
    }

    /**
     * Join a player to a room.
     * Automatically leaves previous room if in one.
     *
     * @param playerId Player UUID
     * @param roomId Room ID to join
     * @throws IllegalArgumentException if room doesn't exist
     */
    public void joinRoom(UUID playerId, String roomId) {
        XocDiaRoom room = rooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room '" + roomId + "' does not exist");
        }

        // Leave current room if in one
        String currentRoom = playerRooms.get(playerId);
        if (currentRoom != null) {
            XocDiaRoom oldRoom = rooms.get(currentRoom);
            if (oldRoom != null) {
                oldRoom.removePlayer(playerId);
            }
        }

        // Join new room
        room.addPlayer(playerId);
        playerRooms.put(playerId, roomId);
    }

    /**
     * Remove player from their current room.
     *
     * @param playerId Player UUID
     */
    public void leaveRoom(UUID playerId) {
        String roomId = playerRooms.remove(playerId);
        if (roomId != null) {
            XocDiaRoom room = rooms.get(roomId);
            if (room != null) {
                room.removePlayer(playerId);
            }
        }
    }

    /**
     * Get room that player is currently in.
     *
     * @param playerId Player UUID
     * @return Room ID or null if not in any room
     */
    public String getPlayerRoom(UUID playerId) {
        return playerRooms.get(playerId);
    }

    /**
     * Check if player is in a room.
     *
     * @param playerId Player UUID
     * @return true if player is in a room
     */
    public boolean isPlayerInRoom(UUID playerId) {
        return playerRooms.containsKey(playerId);
    }

    /**
     * Get session for a specific room.
     *
     * @param roomId Room ID
     * @return Current session or null
     */
    public GameSession getRoomSession(String roomId) {
        XocDiaRoom room = rooms.get(roomId);
        return room != null ? room.getCurrentSession() : null;
    }

    /**
     * Set session for a specific room.
     *
     * @param roomId Room ID
     * @param session Game session
     */
    public void setRoomSession(String roomId, GameSession session) {
        XocDiaRoom room = rooms.get(roomId);
        if (room != null) {
            room.setCurrentSession(session);
        }
    }

    /**
     * Clean up player on disconnect.
     *
     * @param playerId Player UUID
     */
    public void cleanupPlayer(UUID playerId) {
        leaveRoom(playerId);
    }
}
