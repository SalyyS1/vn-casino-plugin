package vn.casino.game.baucua;

import vn.casino.game.engine.BetType;
import vn.casino.game.engine.GameResult;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Bau Cua game result containing 3 dice outcomes.
 * Each die shows one of 6 animals, players win based on matches.
 *
 * Example results:
 * - Bầu-Bầu-Bầu (triple) = 3x payout for Bầu bets
 * - Cua-Cua-Tôm (double) = 2x payout for Cua bets, 1x for Tôm
 * - Bầu-Cua-Tôm (all different) = 1x payout for each animal
 */
public class BauCuaResult {

    private final String serverSeed;
    private final String serverSeedHash;
    private final BauCuaBetType dice1;
    private final BauCuaBetType dice2;
    private final BauCuaBetType dice3;
    private final Map<BauCuaBetType, Integer> matchCounts;
    private final GameResult gameResult;

    /**
     * Create Bau Cua result from 3 dice values.
     *
     * @param serverSeed Server seed used for RNG
     * @param serverSeedHash Committed hash before betting
     * @param d1 First die value [1, 6]
     * @param d2 Second die value [1, 6]
     * @param d3 Third die value [1, 6]
     */
    public BauCuaResult(String serverSeed, String serverSeedHash, int d1, int d2, int d3) {
        this.serverSeed = serverSeed;
        this.serverSeedHash = serverSeedHash;
        this.dice1 = BauCuaBetType.fromDiceValue(d1);
        this.dice2 = BauCuaBetType.fromDiceValue(d2);
        this.dice3 = BauCuaBetType.fromDiceValue(d3);
        this.matchCounts = countMatches();
        this.gameResult = createGameResult();
    }

    /**
     * Count how many times each animal appears in the 3 dice.
     *
     * @return Map of animal to match count (1-3)
     */
    private Map<BauCuaBetType, Integer> countMatches() {
        Map<BauCuaBetType, Integer> counts = new EnumMap<>(BauCuaBetType.class);
        counts.merge(dice1, 1, Integer::sum);
        counts.merge(dice2, 1, Integer::sum);
        counts.merge(dice3, 1, Integer::sum);
        return counts;
    }

    /**
     * Create GameResult record for engine compatibility.
     *
     * @return GameResult with winning bets
     */
    private GameResult createGameResult() {
        int[] rawValues = {dice1.getValue(), dice2.getValue(), dice3.getValue()};
        String displayResult = getDisplayResult();
        Set<BetType> winningBets = new HashSet<>(matchCounts.keySet());

        return new GameResult(
            serverSeed,
            serverSeedHash,
            rawValues,
            displayResult,
            winningBets
        );
    }

    /**
     * Get number of matches for a specific animal.
     *
     * @param animal Animal bet type
     * @return Match count (0-3)
     */
    public int getMatchCount(BauCuaBetType animal) {
        return matchCounts.getOrDefault(animal, 0);
    }

    /**
     * Check if result is a triple (all 3 dice same).
     *
     * @return true if all 3 dice show same animal
     */
    public boolean isTriple() {
        return dice1 == dice2 && dice2 == dice3;
    }

    /**
     * Get human-readable result string.
     * Format: "Animal1 - Animal2 - Animal3"
     *
     * @return Display string (e.g., "Bầu - Cua - Tôm")
     */
    public String getDisplayResult() {
        return String.format("%s - %s - %s",
            dice1.getDisplayName().split(" ")[0],  // Just get the Vietnamese name part
            dice2.getDisplayName().split(" ")[0],
            dice3.getDisplayName().split(" ")[0]
        );
    }

    /**
     * Get first die result.
     *
     * @return First die animal
     */
    public BauCuaBetType getDice1() {
        return dice1;
    }

    /**
     * Get second die result.
     *
     * @return Second die animal
     */
    public BauCuaBetType getDice2() {
        return dice2;
    }

    /**
     * Get third die result.
     *
     * @return Third die animal
     */
    public BauCuaBetType getDice3() {
        return dice3;
    }

    /**
     * Get GameResult record for engine compatibility.
     *
     * @return GameResult
     */
    public GameResult toGameResult() {
        return gameResult;
    }

    /**
     * Get server seed (revealed after result).
     *
     * @return Server seed
     */
    public String getServerSeed() {
        return serverSeed;
    }

    /**
     * Get server seed hash (committed before betting).
     *
     * @return SHA-256 hash
     */
    public String getServerSeedHash() {
        return serverSeedHash;
    }

    /**
     * Get all match counts.
     *
     * @return Map of animals to their match counts
     */
    public Map<BauCuaBetType, Integer> getMatchCounts() {
        return new EnumMap<>(matchCounts);
    }
}
