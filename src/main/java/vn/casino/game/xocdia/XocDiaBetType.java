package vn.casino.game.xocdia;

import vn.casino.game.engine.BetType;

/**
 * Bet types for Xoc Dia (Shake the Plate) game.
 * 4 discs, each red or white, 7 bet types based on red count.
 *
 * Bet types:
 * - CHAN: Even (0, 2, 4 red) - 1.0x payout
 * - LE: Odd (1, 3 red) - 1.0x payout
 * - DO_4: 4 red discs - 10.0x payout
 * - TRANG_4: 4 white discs (0 red) - 10.0x payout
 * - DO_3_TRANG_1: 3 red, 1 white - 2.45x payout
 * - DO_1_TRANG_3: 1 red, 3 white - 2.45x payout
 * - DO_2_TRANG_2: 2 red, 2 white - 2.0x payout
 */
public enum XocDiaBetType implements BetType {
    CHAN("chan", "Chan (Even)", 1.0),
    LE("le", "Le (Odd)", 1.0),
    DO_4("4do", "4 Do", 10.0),
    TRANG_4("4trang", "4 Trang", 10.0),
    DO_3_TRANG_1("3d1t", "3D 1T", 2.45),
    DO_1_TRANG_3("1d3t", "1D 3T", 2.45),
    DO_2_TRANG_2("2d2t", "2D 2T", 2.0);

    private final String id;
    private final String displayName;
    private final double payoutMultiplier;

    XocDiaBetType(String id, String displayName, double payoutMultiplier) {
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
     * Check if this bet type wins for given red count.
     *
     * @param redCount Number of red discs (0-4)
     * @return true if this bet wins
     */
    public boolean winsForRedCount(int redCount) {
        return switch (this) {
            case CHAN -> redCount == 0 || redCount == 2 || redCount == 4;
            case LE -> redCount == 1 || redCount == 3;
            case DO_4 -> redCount == 4;
            case TRANG_4 -> redCount == 0;
            case DO_3_TRANG_1 -> redCount == 3;
            case DO_1_TRANG_3 -> redCount == 1;
            case DO_2_TRANG_2 -> redCount == 2;
        };
    }
}
