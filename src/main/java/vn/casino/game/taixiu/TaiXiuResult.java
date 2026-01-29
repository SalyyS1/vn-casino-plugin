package vn.casino.game.taixiu;

import vn.casino.game.engine.BetType;
import vn.casino.game.engine.GameResult;

import java.util.HashSet;
import java.util.Set;

/**
 * Tai Xiu game result containing 3 dice values and calculated outcome.
 *
 * Rules:
 * - 3 dice, each 1-6
 * - Total range: 3-18
 * - TAI (Big): total >= 11
 * - XIU (Small): total <= 10
 * - Triple (all 3 same): House wins, no payouts
 */
public class TaiXiuResult {

    private final GameResult gameResult;
    private final int dice1;
    private final int dice2;
    private final int dice3;
    private final int total;
    private final boolean isTriple;
    private final TaiXiuBetType winner; // null if triple

    /**
     * Create Tai Xiu result from dice values.
     *
     * @param serverSeed Server seed used for RNG
     * @param serverSeedHash SHA-256 hash of server seed
     * @param dice1 First dice value (1-6)
     * @param dice2 Second dice value (1-6)
     * @param dice3 Third dice value (1-6)
     */
    public TaiXiuResult(
        String serverSeed,
        String serverSeedHash,
        int dice1,
        int dice2,
        int dice3
    ) {
        this.gameResult = new GameResult(
            serverSeed,
            serverSeedHash,
            new int[]{dice1, dice2, dice3},
            formatDisplayResult(dice1, dice2, dice3),
            calculateWinningBets(dice1, dice2, dice3)
        );

        this.dice1 = dice1;
        this.dice2 = dice2;
        this.dice3 = dice3;
        this.total = dice1 + dice2 + dice3;
        this.isTriple = (dice1 == dice2 && dice2 == dice3);

        // Determine winner (null if triple)
        if (isTriple) {
            this.winner = null;
        } else {
            this.winner = total >= 11 ? TaiXiuBetType.TAI : TaiXiuBetType.XIU;
        }
    }

    /**
     * Format display result string.
     *
     * @param d1 First dice
     * @param d2 Second dice
     * @param d3 Third dice
     * @return Formatted result string
     */
    private static String formatDisplayResult(int d1, int d2, int d3) {
        int total = d1 + d2 + d3;
        boolean isTriple = (d1 == d2 && d2 == d3);

        if (isTriple) {
            return String.format("BA %d (%d + %d + %d = %d) - NHÀ CÁI THẮNG",
                d1, d1, d2, d3, total);
        }

        String outcome = total >= 11 ? "TÀI" : "XỈU";
        return String.format("%s (%d + %d + %d = %d)",
            outcome, d1, d2, d3, total);
    }

    /**
     * Calculate winning bet types.
     *
     * @param d1 First dice
     * @param d2 Second dice
     * @param d3 Third dice
     * @return Set of winning bet types (empty if triple)
     */
    private static Set<BetType> calculateWinningBets(int d1, int d2, int d3) {
        Set<BetType> winners = new HashSet<>();

        // Triple = house wins, no winning bets
        if (d1 == d2 && d2 == d3) {
            return winners;
        }

        int total = d1 + d2 + d3;
        if (total >= 11) {
            winners.add(TaiXiuBetType.TAI);
        } else {
            winners.add(TaiXiuBetType.XIU);
        }

        return winners;
    }

    // Getters

    public int getDice1() {
        return dice1;
    }

    public int getDice2() {
        return dice2;
    }

    public int getDice3() {
        return dice3;
    }

    public int getTotal() {
        return total;
    }

    public boolean isTriple() {
        return isTriple;
    }

    public TaiXiuBetType getWinner() {
        return winner;
    }

    /**
     * Check if this is a TAI (Big) result.
     *
     * @return true if TAI won
     */
    public boolean isTai() {
        return !isTriple && total >= 11;
    }

    /**
     * Check if this is a XIU (Small) result.
     *
     * @return true if XIU won
     */
    public boolean isXiu() {
        return !isTriple && total <= 10;
    }

    /**
     * Convert to generic GameResult.
     *
     * @return GameResult representation
     */
    public GameResult toGameResult() {
        return gameResult;
    }

    /**
     * Get display result string.
     *
     * @return Formatted result string
     */
    public String displayResult() {
        return gameResult.displayResult();
    }

    /**
     * Get server seed.
     *
     * @return Server seed
     */
    public String getServerSeed() {
        return gameResult.serverSeed();
    }

    /**
     * Get server seed hash.
     *
     * @return Server seed hash
     */
    public String getServerSeedHash() {
        return gameResult.serverSeedHash();
    }

    /**
     * Get raw values.
     *
     * @return Raw dice values
     */
    public int[] getRawValues() {
        return gameResult.rawValues();
    }

    /**
     * Get display result (alias for displayResult).
     *
     * @return Display result string
     */
    public String getDisplayResult() {
        return gameResult.displayResult();
    }

    /**
     * Get winning bets.
     *
     * @return Set of winning bet types
     */
    public java.util.Set<vn.casino.game.engine.BetType> getWinningBets() {
        return gameResult.winningBets();
    }
}
