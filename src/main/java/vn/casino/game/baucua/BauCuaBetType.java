package vn.casino.game.baucua;

import org.bukkit.Material;
import vn.casino.game.engine.BetType;

/**
 * Bau Cua bet types - 6 animals on 3 dice.
 * Each die shows one of 6 animals: Gourd, Crab, Shrimp, Fish, Deer, Rooster.
 *
 * Payout: 1:1 per matching die
 * - 1 match: 1x payout (return bet + 1x)
 * - 2 matches: 2x payout (return bet + 2x)
 * - 3 matches (triple): 3x payout (return bet + 3x)
 *
 * House edge: Built into single-die probabilities (each animal has 1/6 chance per die)
 */
public enum BauCuaBetType implements BetType {
    BAU("bau", "Bầu (Gourd)", 1, Material.MELON),
    CUA("cua", "Cua (Crab)", 2, Material.RED_DYE),
    TOM("tom", "Tôm (Shrimp)", 3, Material.PINK_DYE),
    CA("ca", "Cá (Fish)", 4, Material.TROPICAL_FISH),
    NAI("nai", "Nai (Deer)", 5, Material.LEATHER),
    GA("ga", "Gà (Rooster)", 6, Material.FEATHER);

    private final String id;
    private final String displayName;
    private final int value;  // 1-6 for dice value mapping
    private final Material icon;

    BauCuaBetType(String id, String displayName, int value, Material icon) {
        this.id = id;
        this.displayName = displayName;
        this.value = value;
        this.icon = icon;
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
        // Base multiplier is 1.0 per match
        // Actual payout is calculated by: bet * matches * 1.0
        // Example: 1000 VND bet, 2 matches = 1000 + (1000 * 2 * 1.0) = 3000 VND total
        return 1.0;
    }

    /**
     * Get dice value for this animal.
     *
     * @return Value [1, 6]
     */
    public int getValue() {
        return value;
    }

    /**
     * Get Minecraft material icon for GUI display.
     *
     * @return Material for this animal
     */
    public Material getIcon() {
        return icon;
    }

    /**
     * Get bet type from dice value (1-6).
     *
     * @param value Dice value
     * @return Corresponding animal bet type
     * @throws IllegalArgumentException if value is not in range [1, 6]
     */
    public static BauCuaBetType fromDiceValue(int value) {
        if (value < 1 || value > 6) {
            throw new IllegalArgumentException("Dice value must be between 1 and 6");
        }
        return values()[value - 1];
    }

    /**
     * Get bet type from ID string.
     *
     * @param id Bet type ID
     * @return Bet type or null if not found
     */
    public static BauCuaBetType fromId(String id) {
        for (BauCuaBetType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }
}
