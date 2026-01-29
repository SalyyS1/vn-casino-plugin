package vn.casino.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import vn.casino.game.engine.ProvablyFairRNG;
import vn.casino.game.taixiu.TaiXiuBetType;
import vn.casino.game.taixiu.TaiXiuResult;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Tai Xiu game result and payout calculation.
 * Tests Tai/Xiu wins, triple detection, and payout logic.
 */
@DisplayName("TaiXiu Payout Tests")
class TaiXiuPayoutTest {

    private String serverSeed;
    private String serverSeedHash;

    @BeforeEach
    void setUp() {
        serverSeed = "test-server-seed";
        serverSeedHash = ProvablyFairRNG.commitment(serverSeed);
    }

    @Test
    @DisplayName("Tai should win when total >= 11")
    void testTaiWins() {
        TaiXiuResult result = new TaiXiuResult(serverSeed, serverSeedHash, 4, 4, 4); // 12

        assertEquals(12, result.getTotal());
        assertEquals(TaiXiuBetType.TAI, result.getWinner());
        assertTrue(result.isTai());
        assertFalse(result.isXiu());
        assertFalse(result.isTriple());
        assertTrue(result.getWinningBets().contains(TaiXiuBetType.TAI));
        assertFalse(result.getWinningBets().contains(TaiXiuBetType.XIU));
    }

    @Test
    @DisplayName("Xiu should win when total <= 10")
    void testXiuWins() {
        TaiXiuResult result = new TaiXiuResult(serverSeed, serverSeedHash, 1, 2, 3); // 6

        assertEquals(6, result.getTotal());
        assertEquals(TaiXiuBetType.XIU, result.getWinner());
        assertTrue(result.isXiu());
        assertFalse(result.isTai());
        assertFalse(result.isTriple());
        assertTrue(result.getWinningBets().contains(TaiXiuBetType.XIU));
        assertFalse(result.getWinningBets().contains(TaiXiuBetType.TAI));
    }

    @Test
    @DisplayName("Triple should result in no winners (house wins)")
    void testTriple() {
        TaiXiuResult result = new TaiXiuResult(serverSeed, serverSeedHash, 3, 3, 3); // 9

        assertEquals(9, result.getTotal());
        assertTrue(result.isTriple());
        assertNull(result.getWinner(), "Triple should have no winner");
        assertFalse(result.isTai());
        assertFalse(result.isXiu());
        assertTrue(result.getWinningBets().isEmpty(), "No bets should win on triple");
    }

    @ParameterizedTest
    @DisplayName("Test all Tai outcomes (total >= 11)")
    @CsvSource({
        "6, 5, 0, 11",   // Minimum Tai (but 0 is invalid dice)
        "4, 4, 3, 11",   // Minimum valid Tai
        "5, 4, 3, 12",
        "5, 5, 3, 13",
        "5, 5, 4, 14",
        "5, 5, 5, 15",
        "6, 5, 5, 16",
        "6, 6, 5, 17",
        "6, 6, 6, 18"    // Maximum but triple
    })
    void testTaiOutcomes(int d1, int d2, int d3, int expectedTotal) {
        if (d1 < 1 || d2 < 1 || d3 < 1) return; // Skip invalid dice values

        TaiXiuResult result = new TaiXiuResult(serverSeed, serverSeedHash, d1, d2, d3);

        assertEquals(expectedTotal, result.getTotal());
        if (!result.isTriple()) {
            assertEquals(TaiXiuBetType.TAI, result.getWinner());
            assertTrue(result.isTai());
        }
    }

    @ParameterizedTest
    @DisplayName("Test all Xiu outcomes (total <= 10)")
    @CsvSource({
        "1, 1, 1, 3",    // Minimum (triple)
        "1, 1, 2, 4",
        "1, 2, 2, 5",
        "2, 2, 2, 6",    // Triple
        "1, 3, 3, 7",
        "2, 3, 3, 8",
        "3, 3, 3, 9",    // Triple
        "2, 4, 4, 10"    // Maximum Xiu
    })
    void testXiuOutcomes(int d1, int d2, int d3, int expectedTotal) {
        TaiXiuResult result = new TaiXiuResult(serverSeed, serverSeedHash, d1, d2, d3);

        assertEquals(expectedTotal, result.getTotal());
        if (!result.isTriple()) {
            assertEquals(TaiXiuBetType.XIU, result.getWinner());
            assertTrue(result.isXiu());
        }
    }

    @ParameterizedTest
    @DisplayName("Test all triple outcomes")
    @CsvSource({
        "1, 1, 1, 3",
        "2, 2, 2, 6",
        "3, 3, 3, 9",
        "4, 4, 4, 12",
        "5, 5, 5, 15",
        "6, 6, 6, 18"
    })
    void testAllTriples(int d1, int d2, int d3, int expectedTotal) {
        TaiXiuResult result = new TaiXiuResult(serverSeed, serverSeedHash, d1, d2, d3);

        assertEquals(expectedTotal, result.getTotal());
        assertTrue(result.isTriple(), "Should detect triple");
        assertNull(result.getWinner(), "Triple should have no winner");
        assertTrue(result.getWinningBets().isEmpty());
    }

    @Test
    @DisplayName("Boundary case: total = 10 should be Xiu")
    void testBoundaryXiu() {
        TaiXiuResult result = new TaiXiuResult(serverSeed, serverSeedHash, 2, 4, 4); // 10

        assertEquals(10, result.getTotal());
        assertEquals(TaiXiuBetType.XIU, result.getWinner());
        assertTrue(result.isXiu());
        assertFalse(result.isTai());
    }

    @Test
    @DisplayName("Boundary case: total = 11 should be Tai")
    void testBoundaryTai() {
        TaiXiuResult result = new TaiXiuResult(serverSeed, serverSeedHash, 4, 4, 3); // 11

        assertEquals(11, result.getTotal());
        assertEquals(TaiXiuBetType.TAI, result.getWinner());
        assertTrue(result.isTai());
        assertFalse(result.isXiu());
    }

    @Test
    @DisplayName("Result should store dice values correctly")
    void testDiceStorage() {
        TaiXiuResult result = new TaiXiuResult(serverSeed, serverSeedHash, 2, 4, 5);

        assertEquals(2, result.getDice1());
        assertEquals(4, result.getDice2());
        assertEquals(5, result.getDice3());
        assertEquals(11, result.getTotal());
    }

    @Test
    @DisplayName("Result should store server seed and hash")
    void testSeedStorage() {
        TaiXiuResult result = new TaiXiuResult(serverSeed, serverSeedHash, 1, 2, 3);

        assertEquals(serverSeed, result.getServerSeed());
        assertEquals(serverSeedHash, result.getServerSeedHash());
    }

    @Test
    @DisplayName("Display result should be formatted correctly")
    void testDisplayResult() {
        TaiXiuResult taiResult = new TaiXiuResult(serverSeed, serverSeedHash, 4, 4, 3);
        String taiDisplay = taiResult.getDisplayResult();
        assertTrue(taiDisplay.contains("TÀI") || taiDisplay.contains("11"),
            "Tai result should contain TÀI or total");

        TaiXiuResult xiuResult = new TaiXiuResult(serverSeed, serverSeedHash, 1, 2, 3);
        String xiuDisplay = xiuResult.getDisplayResult();
        assertTrue(xiuDisplay.contains("XỈU") || xiuDisplay.contains("6"),
            "Xiu result should contain XỈU or total");

        TaiXiuResult tripleResult = new TaiXiuResult(serverSeed, serverSeedHash, 3, 3, 3);
        String tripleDisplay = tripleResult.getDisplayResult();
        assertTrue(tripleDisplay.contains("BA") || tripleDisplay.contains("NHÀ CÁI"),
            "Triple result should indicate house win");
    }

    @Test
    @DisplayName("Raw values should match dice")
    void testRawValues() {
        TaiXiuResult result = new TaiXiuResult(serverSeed, serverSeedHash, 2, 4, 5);
        int[] rawValues = result.getRawValues();

        assertEquals(3, rawValues.length);
        assertEquals(2, rawValues[0]);
        assertEquals(4, rawValues[1]);
        assertEquals(5, rawValues[2]);
    }

    @Test
    @DisplayName("Verify provably fair properties")
    void testProvablyFairProperties() {
        String seed = ProvablyFairRNG.generateServerSeed();
        String hash = ProvablyFairRNG.commitment(seed);

        TaiXiuResult result = new TaiXiuResult(seed, hash, 3, 4, 5);

        // Verify seed can be validated
        assertTrue(ProvablyFairRNG.verify(result.getServerSeed(), result.getServerSeedHash()),
            "Server seed should be verifiable");
    }

    @Test
    @DisplayName("Minimum total should be 3")
    void testMinimumTotal() {
        TaiXiuResult result = new TaiXiuResult(serverSeed, serverSeedHash, 1, 1, 1);
        assertEquals(3, result.getTotal());
        assertTrue(result.isTriple());
    }

    @Test
    @DisplayName("Maximum total should be 18")
    void testMaximumTotal() {
        TaiXiuResult result = new TaiXiuResult(serverSeed, serverSeedHash, 6, 6, 6);
        assertEquals(18, result.getTotal());
        assertTrue(result.isTriple());
    }
}
