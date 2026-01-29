package vn.casino.game.xocdia;

import vn.casino.game.jackpot.JackpotConfig;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for Xoc Dia game.
 * Includes round timing, rooms, payouts, and jackpot settings.
 */
public class XocDiaConfig {
    private Duration roundDuration;
    private Duration bettingDuration;
    private List<RoomConfig> rooms;
    private JackpotConfig jackpot;

    /**
     * Room configuration record.
     *
     * @param id Room identifier
     * @param displayName Display name for GUI
     * @param minBet Minimum bet amount
     * @param maxBet Maximum bet amount
     */
    public record RoomConfig(
        String id,
        String displayName,
        BigDecimal minBet,
        BigDecimal maxBet
    ) {
        public RoomConfig {
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
        }
    }

    /**
     * Create default configuration.
     *
     * @return Default Xoc Dia config
     */
    public static XocDiaConfig createDefault() {
        XocDiaConfig config = new XocDiaConfig();
        config.roundDuration = Duration.ofSeconds(60);
        config.bettingDuration = Duration.ofSeconds(50);
        config.rooms = createDefaultRooms();
        config.jackpot = JackpotConfig.createDefault();
        return config;
    }

    /**
     * Create default room list.
     *
     * @return List of default rooms
     */
    private static List<RoomConfig> createDefaultRooms() {
        List<RoomConfig> rooms = new ArrayList<>();

        rooms.add(new RoomConfig(
            "room1",
            "Phòng 1 - Nghèo",
            new BigDecimal("1000"),
            new BigDecimal("100000")
        ));

        rooms.add(new RoomConfig(
            "room2",
            "Phòng 2 - Trung Bình",
            new BigDecimal("10000"),
            new BigDecimal("1000000")
        ));

        rooms.add(new RoomConfig(
            "room3",
            "Phòng 3 - Đại Gia",
            new BigDecimal("100000"),
            new BigDecimal("10000000")
        ));

        rooms.add(new RoomConfig(
            "vip",
            "Phòng VIP",
            new BigDecimal("1000000"),
            new BigDecimal("100000000")
        ));

        return rooms;
    }

    // Getters and setters
    public Duration getRoundDuration() {
        return roundDuration;
    }

    public void setRoundDuration(Duration roundDuration) {
        this.roundDuration = roundDuration;
    }

    public Duration getBettingDuration() {
        return bettingDuration;
    }

    public void setBettingDuration(Duration bettingDuration) {
        this.bettingDuration = bettingDuration;
    }

    public List<RoomConfig> getRooms() {
        return rooms;
    }

    public void setRooms(List<RoomConfig> rooms) {
        this.rooms = rooms;
    }

    public JackpotConfig getJackpot() {
        return jackpot;
    }

    public void setJackpot(JackpotConfig jackpot) {
        this.jackpot = jackpot;
    }
}
