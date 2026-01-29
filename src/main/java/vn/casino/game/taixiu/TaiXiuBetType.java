package vn.casino.game.taixiu;

import vn.casino.game.engine.BetType;

/**
 * Tai Xiu bet type enum.
 * Two possible bet types: TAI (Big >= 11) and XIU (Small <= 10).
 * Both have 1.98x payout (approximately 1% house edge).
 *
 * Triple (all 3 dice same) results in house win (both sides lose).
 */
public enum TaiXiuBetType implements BetType {
    /**
     * TAI (Big) - Total dice sum >= 11.
     * Payout: 1.98x
     */
    TAI("tai", "Tài", 1.98),

    /**
     * XIU (Small) - Total dice sum <= 10.
     * Payout: 1.98x
     */
    XIU("xiu", "Xỉu", 1.98);

    private final String id;
    private final String displayName;
    private final double payoutMultiplier;

    TaiXiuBetType(String id, String displayName, double payoutMultiplier) {
        this.id = id;
        this.displayName = displayName;
        this.payoutMultiplier = payoutMultiplier;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public double getPayoutMultiplier() {
        return payoutMultiplier;
    }

    /**
     * Get bet type from ID string.
     *
     * @param id Bet type ID
     * @return Matching bet type
     * @throws IllegalArgumentException if ID is invalid
     */
    public static TaiXiuBetType fromId(String id) {
        for (TaiXiuBetType type : values()) {
            if (type.getId().equalsIgnoreCase(id)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid TaiXiu bet type: " + id);
    }
}
