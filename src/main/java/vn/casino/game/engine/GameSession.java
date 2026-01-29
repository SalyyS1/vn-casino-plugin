package vn.casino.game.engine;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Game session representing a single round of a casino game.
 * Each session has a unique server seed for provably fair RNG.
 * Thread-safe for concurrent bet placement.
 */
public class GameSession {
    private final long id;
    private final String gameId;
    private final String room; // Null for non-room games (Tai Xiu, Bau Cua)
    private GameSessionState state;
    private final String serverSeed;
    private final String serverSeedHash;
    private final Map<UUID, List<Bet>> bets; // Player UUID -> List of bets
    private GameResult result;
    private final Instant startedAt;
    private Instant endedAt;

    public GameSession(
        long id,
        String gameId,
        String room,
        String serverSeed,
        String serverSeedHash
    ) {
        this.id = id;
        this.gameId = gameId;
        this.room = room;
        this.state = GameSessionState.WAITING;
        this.serverSeed = serverSeed;
        this.serverSeedHash = serverSeedHash;
        this.bets = new ConcurrentHashMap<>();
        this.result = null;
        this.startedAt = Instant.now();
        this.endedAt = null;
    }

    /**
     * Add a bet to this session.
     * Thread-safe for concurrent bet placement.
     *
     * @param bet Bet to add
     */
    public void addBet(Bet bet) {
        if (state != GameSessionState.BETTING) {
            throw new IllegalStateException("Cannot add bet when session is not in BETTING state");
        }
        bets.computeIfAbsent(bet.playerId(), k -> new ArrayList<>()).add(bet);
    }

    /**
     * Get all bets for a specific player.
     *
     * @param playerId Player UUID
     * @return List of player's bets (unmodifiable)
     */
    public List<Bet> getPlayerBets(UUID playerId) {
        return Collections.unmodifiableList(bets.getOrDefault(playerId, Collections.emptyList()));
    }

    /**
     * Get all bets in this session.
     *
     * @return List of all bets (unmodifiable)
     */
    public List<Bet> getAllBets() {
        List<Bet> allBets = new ArrayList<>();
        bets.values().forEach(allBets::addAll);
        return Collections.unmodifiableList(allBets);
    }

    /**
     * Get set of all players who placed bets.
     *
     * @return Set of player UUIDs
     */
    public Set<UUID> getPlayers() {
        return Collections.unmodifiableSet(bets.keySet());
    }

    /**
     * Calculate total bet amount for a specific player.
     *
     * @param playerId Player UUID
     * @return Total bet amount
     */
    public BigDecimal getTotalBetAmount(UUID playerId) {
        return getPlayerBets(playerId).stream()
            .map(Bet::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate total bet amount across all players.
     *
     * @return Total bet amount
     */
    public BigDecimal getTotalBetAmount() {
        return getAllBets().stream()
            .map(Bet::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Check if a player has already placed a bet in this session.
     *
     * @param playerId Player UUID
     * @return true if player has bets
     */
    public boolean hasPlayerBet(UUID playerId) {
        return bets.containsKey(playerId);
    }

    /**
     * Update session state.
     *
     * @param newState New state
     */
    public void setState(GameSessionState newState) {
        this.state = newState;
        if (newState == GameSessionState.ENDED && endedAt == null) {
            this.endedAt = Instant.now();
        }
    }

    /**
     * Set session result.
     *
     * @param result Game result
     */
    public void setResult(GameResult result) {
        if (state != GameSessionState.CALCULATING && state != GameSessionState.RESULT) {
            throw new IllegalStateException("Cannot set result when session is not in CALCULATING/RESULT state");
        }
        this.result = result;
    }

    // Getters
    public long getId() {
        return id;
    }

    public String getGameId() {
        return gameId;
    }

    public String getRoom() {
        return room;
    }

    public GameSessionState getState() {
        return state;
    }

    public String getServerSeed() {
        return serverSeed;
    }

    public String getServerSeedHash() {
        return serverSeedHash;
    }

    public GameResult getResult() {
        return result;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    /**
     * Check if this is a room-based game session.
     *
     * @return true if room-based (Xoc Dia)
     */
    public boolean isRoomBased() {
        return room != null;
    }
}
