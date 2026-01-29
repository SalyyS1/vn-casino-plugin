package vn.casino.game.xocdia;

import vn.casino.game.engine.BetType;
import vn.casino.game.engine.GameResult;

import java.util.HashSet;
import java.util.Set;

/**
 * Result for Xoc Dia game.
 * Contains 4 disc states (red/white) and calculated winners.
 */
public class XocDiaResult {
    private final GameResult gameResult;
    private final boolean[] discs; // true = red, false = white
    private final int redCount;

    /**
     * Create Xoc Dia result from disc states.
     *
     * @param serverSeed Server seed used for RNG
     * @param serverSeedHash SHA-256 hash of server seed
     * @param discs Array of 4 disc states (true=red, false=white)
     */
    public XocDiaResult(String serverSeed, String serverSeedHash, boolean[] discs) {
        if (discs.length != 4) {
            throw new IllegalArgumentException("Xoc Dia requires exactly 4 discs");
        }

        this.gameResult = new GameResult(
            serverSeed,
            serverSeedHash,
            convertToRawValues(discs),
            buildDisplayResult(discs),
            determineWinners(countRed(discs))
        );

        this.discs = discs.clone();
        this.redCount = countRed(discs);
    }

    /**
     * Get disc states.
     *
     * @return Array of 4 disc states (true=red, false=white)
     */
    public boolean[] getDiscs() {
        return discs.clone();
    }

    /**
     * Get number of red discs.
     *
     * @return Red count (0-4)
     */
    public int getRedCount() {
        return redCount;
    }

    /**
     * Count red discs.
     *
     * @param discs Disc array
     * @return Number of red discs
     */
    private static int countRed(boolean[] discs) {
        int count = 0;
        for (boolean disc : discs) {
            if (disc) count++;
        }
        return count;
    }

    /**
     * Convert disc states to raw values for GameResult.
     * 1 = red, 0 = white
     *
     * @param discs Disc states
     * @return Raw values array
     */
    private static int[] convertToRawValues(boolean[] discs) {
        int[] values = new int[discs.length];
        for (int i = 0; i < discs.length; i++) {
            values[i] = discs[i] ? 1 : 0;
        }
        return values;
    }

    /**
     * Build display result string.
     *
     * @param discs Disc states
     * @return Display string (e.g., "4 Đỏ", "2 Đỏ 2 Trắng")
     */
    private static String buildDisplayResult(boolean[] discs) {
        int red = countRed(discs);
        int white = 4 - red;

        if (red == 4) {
            return "4 Đỏ";
        } else if (red == 0) {
            return "4 Trắng";
        } else {
            return red + " Đỏ " + white + " Trắng";
        }
    }

    /**
     * Determine winning bet types based on red count.
     *
     * @param redCount Number of red discs
     * @return Set of winning bet types
     */
    private static Set<BetType> determineWinners(int redCount) {
        Set<BetType> winners = new HashSet<>();

        for (XocDiaBetType betType : XocDiaBetType.values()) {
            if (betType.winsForRedCount(redCount)) {
                winners.add(betType);
            }
        }

        return winners;
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
     * @return Raw disc values (1=red, 0=white)
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
