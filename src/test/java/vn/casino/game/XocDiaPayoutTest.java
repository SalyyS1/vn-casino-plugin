package vn.casino.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import vn.casino.game.engine.ProvablyFairRNG;
import vn.casino.game.xocdia.XocDiaBetType;
import vn.casino.game.xocdia.XocDiaResult;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Xoc Dia game result and payout calculation.
 * Tests all 7 bet type payouts based on red disc count.
 */
@DisplayName("XocDia Payout Tests")
class XocDiaPayoutTest {

    private String serverSeed;
    private String serverSeedHash;

    @BeforeEach
    void setUp() {
        serverSeed = "test-server-seed";
        serverSeedHash = ProvablyFairRNG.commitment(serverSeed);
    }

    @Test
    @DisplayName("4 red discs should win DO_4 and CHAN")
    void test4Red() {
        boolean[] discs = {true, true, true, true}; // 4 red
        XocDiaResult result = new XocDiaResult(serverSeed, serverSeedHash, discs);

        assertEquals(4, result.getRedCount());
        assertTrue(result.getWinningBets().contains(XocDiaBetType.DO_4));
        assertTrue(result.getWinningBets().contains(XocDiaBetType.CHAN)); // Even
        assertFalse(result.getWinningBets().contains(XocDiaBetType.LE)); // Not odd
        assertFalse(result.getWinningBets().contains(XocDiaBetType.TRANG_4));
    }

    @Test
    @DisplayName("0 red (4 white) should win TRANG_4 and CHAN")
    void test0Red() {
        boolean[] discs = {false, false, false, false}; // 0 red = 4 white
        XocDiaResult result = new XocDiaResult(serverSeed, serverSeedHash, discs);

        assertEquals(0, result.getRedCount());
        assertTrue(result.getWinningBets().contains(XocDiaBetType.TRANG_4));
        assertTrue(result.getWinningBets().contains(XocDiaBetType.CHAN)); // Even
        assertFalse(result.getWinningBets().contains(XocDiaBetType.LE)); // Not odd
        assertFalse(result.getWinningBets().contains(XocDiaBetType.DO_4));
    }

    @Test
    @DisplayName("3 red should win DO_3_TRANG_1 and LE")
    void test3Red() {
        boolean[] discs = {true, true, true, false}; // 3 red, 1 white
        XocDiaResult result = new XocDiaResult(serverSeed, serverSeedHash, discs);

        assertEquals(3, result.getRedCount());
        assertTrue(result.getWinningBets().contains(XocDiaBetType.DO_3_TRANG_1));
        assertTrue(result.getWinningBets().contains(XocDiaBetType.LE)); // Odd
        assertFalse(result.getWinningBets().contains(XocDiaBetType.CHAN)); // Not even
    }

    @Test
    @DisplayName("1 red should win DO_1_TRANG_3 and LE")
    void test1Red() {
        boolean[] discs = {true, false, false, false}; // 1 red, 3 white
        XocDiaResult result = new XocDiaResult(serverSeed, serverSeedHash, discs);

        assertEquals(1, result.getRedCount());
        assertTrue(result.getWinningBets().contains(XocDiaBetType.DO_1_TRANG_3));
        assertTrue(result.getWinningBets().contains(XocDiaBetType.LE)); // Odd
        assertFalse(result.getWinningBets().contains(XocDiaBetType.CHAN)); // Not even
    }

    @Test
    @DisplayName("2 red should win DO_2_TRANG_2 and CHAN")
    void test2Red() {
        boolean[] discs = {true, true, false, false}; // 2 red, 2 white
        XocDiaResult result = new XocDiaResult(serverSeed, serverSeedHash, discs);

        assertEquals(2, result.getRedCount());
        assertTrue(result.getWinningBets().contains(XocDiaBetType.DO_2_TRANG_2));
        assertTrue(result.getWinningBets().contains(XocDiaBetType.CHAN)); // Even
        assertFalse(result.getWinningBets().contains(XocDiaBetType.LE)); // Not odd
    }

    @ParameterizedTest
    @DisplayName("Test CHAN (even) bet type wins for 0, 2, 4 red")
    @CsvSource({
        "0, true",
        "1, false",
        "2, true",
        "3, false",
        "4, true"
    })
    void testChanBetType(int redCount, boolean shouldWin) {
        assertEquals(shouldWin, XocDiaBetType.CHAN.winsForRedCount(redCount));
    }

    @ParameterizedTest
    @DisplayName("Test LE (odd) bet type wins for 1, 3 red")
    @CsvSource({
        "0, false",
        "1, true",
        "2, false",
        "3, true",
        "4, false"
    })
    void testLeBetType(int redCount, boolean shouldWin) {
        assertEquals(shouldWin, XocDiaBetType.LE.winsForRedCount(redCount));
    }

    @Test
    @DisplayName("DO_4 should only win with 4 red")
    void testDo4BetType() {
        assertTrue(XocDiaBetType.DO_4.winsForRedCount(4));
        assertFalse(XocDiaBetType.DO_4.winsForRedCount(3));
        assertFalse(XocDiaBetType.DO_4.winsForRedCount(2));
        assertFalse(XocDiaBetType.DO_4.winsForRedCount(1));
        assertFalse(XocDiaBetType.DO_4.winsForRedCount(0));
    }

    @Test
    @DisplayName("TRANG_4 should only win with 0 red")
    void testTrang4BetType() {
        assertTrue(XocDiaBetType.TRANG_4.winsForRedCount(0));
        assertFalse(XocDiaBetType.TRANG_4.winsForRedCount(1));
        assertFalse(XocDiaBetType.TRANG_4.winsForRedCount(2));
        assertFalse(XocDiaBetType.TRANG_4.winsForRedCount(3));
        assertFalse(XocDiaBetType.TRANG_4.winsForRedCount(4));
    }

    @Test
    @DisplayName("DO_3_TRANG_1 should only win with 3 red")
    void testDo3Trang1BetType() {
        assertTrue(XocDiaBetType.DO_3_TRANG_1.winsForRedCount(3));
        assertFalse(XocDiaBetType.DO_3_TRANG_1.winsForRedCount(0));
        assertFalse(XocDiaBetType.DO_3_TRANG_1.winsForRedCount(1));
        assertFalse(XocDiaBetType.DO_3_TRANG_1.winsForRedCount(2));
        assertFalse(XocDiaBetType.DO_3_TRANG_1.winsForRedCount(4));
    }

    @Test
    @DisplayName("DO_1_TRANG_3 should only win with 1 red")
    void testDo1Trang3BetType() {
        assertTrue(XocDiaBetType.DO_1_TRANG_3.winsForRedCount(1));
        assertFalse(XocDiaBetType.DO_1_TRANG_3.winsForRedCount(0));
        assertFalse(XocDiaBetType.DO_1_TRANG_3.winsForRedCount(2));
        assertFalse(XocDiaBetType.DO_1_TRANG_3.winsForRedCount(3));
        assertFalse(XocDiaBetType.DO_1_TRANG_3.winsForRedCount(4));
    }

    @Test
    @DisplayName("DO_2_TRANG_2 should only win with 2 red")
    void testDo2Trang2BetType() {
        assertTrue(XocDiaBetType.DO_2_TRANG_2.winsForRedCount(2));
        assertFalse(XocDiaBetType.DO_2_TRANG_2.winsForRedCount(0));
        assertFalse(XocDiaBetType.DO_2_TRANG_2.winsForRedCount(1));
        assertFalse(XocDiaBetType.DO_2_TRANG_2.winsForRedCount(3));
        assertFalse(XocDiaBetType.DO_2_TRANG_2.winsForRedCount(4));
    }

    @Test
    @DisplayName("Payout multipliers should match spec")
    void testPayoutMultipliers() {
        assertEquals(1.0, XocDiaBetType.CHAN.getPayoutMultiplier());
        assertEquals(1.0, XocDiaBetType.LE.getPayoutMultiplier());
        assertEquals(10.0, XocDiaBetType.DO_4.getPayoutMultiplier());
        assertEquals(10.0, XocDiaBetType.TRANG_4.getPayoutMultiplier());
        assertEquals(2.45, XocDiaBetType.DO_3_TRANG_1.getPayoutMultiplier());
        assertEquals(2.45, XocDiaBetType.DO_1_TRANG_3.getPayoutMultiplier());
        assertEquals(2.0, XocDiaBetType.DO_2_TRANG_2.getPayoutMultiplier());
    }

    @Test
    @DisplayName("Result should throw on invalid disc count")
    void testInvalidDiscCount() {
        assertThrows(IllegalArgumentException.class, () -> {
            boolean[] discs = {true, true, true}; // Only 3 discs
            new XocDiaResult(serverSeed, serverSeedHash, discs);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            boolean[] discs = {true, true, true, true, true}; // 5 discs
            new XocDiaResult(serverSeed, serverSeedHash, discs);
        });
    }

    @Test
    @DisplayName("Disc states should be stored correctly")
    void testDiscStorage() {
        boolean[] discs = {true, false, true, false};
        XocDiaResult result = new XocDiaResult(serverSeed, serverSeedHash, discs);

        boolean[] stored = result.getDiscs();
        assertEquals(4, stored.length);
        assertEquals(true, stored[0]);
        assertEquals(false, stored[1]);
        assertEquals(true, stored[2]);
        assertEquals(false, stored[3]);
    }

    @Test
    @DisplayName("Display result should be formatted correctly")
    void testDisplayResult() {
        boolean[] fourRed = {true, true, true, true};
        XocDiaResult result4Red = new XocDiaResult(serverSeed, serverSeedHash, fourRed);
        assertEquals("4 Đỏ", result4Red.getDisplayResult());

        boolean[] fourWhite = {false, false, false, false};
        XocDiaResult result4White = new XocDiaResult(serverSeed, serverSeedHash, fourWhite);
        assertEquals("4 Trắng", result4White.getDisplayResult());

        boolean[] twoRed = {true, true, false, false};
        XocDiaResult result2Red = new XocDiaResult(serverSeed, serverSeedHash, twoRed);
        assertEquals("2 Đỏ 2 Trắng", result2Red.getDisplayResult());

        boolean[] threeRed = {true, true, true, false};
        XocDiaResult result3Red = new XocDiaResult(serverSeed, serverSeedHash, threeRed);
        assertEquals("3 Đỏ 1 Trắng", result3Red.getDisplayResult());
    }

    @Test
    @DisplayName("Raw values should represent disc states")
    void testRawValues() {
        boolean[] discs = {true, false, true, false}; // 1, 0, 1, 0
        XocDiaResult result = new XocDiaResult(serverSeed, serverSeedHash, discs);

        int[] rawValues = result.getRawValues();
        assertEquals(4, rawValues.length);
        assertEquals(1, rawValues[0]); // red = 1
        assertEquals(0, rawValues[1]); // white = 0
        assertEquals(1, rawValues[2]); // red = 1
        assertEquals(0, rawValues[3]); // white = 0
    }

    @Test
    @DisplayName("Server seed verification should work")
    void testSeedVerification() {
        boolean[] discs = {true, true, false, false};
        XocDiaResult result = new XocDiaResult(serverSeed, serverSeedHash, discs);

        assertTrue(ProvablyFairRNG.verify(result.getServerSeed(), result.getServerSeedHash()));
    }

    @Test
    @DisplayName("Each red count should have at least 2 winning bet types")
    void testAllRedCountsHaveWinners() {
        for (int redCount = 0; redCount <= 4; redCount++) {
            long winnerCount = 0;
            for (XocDiaBetType betType : XocDiaBetType.values()) {
                if (betType.winsForRedCount(redCount)) {
                    winnerCount++;
                }
            }
            assertTrue(winnerCount >= 2,
                "Red count " + redCount + " should have at least 2 winning bet types");
        }
    }

    @Test
    @DisplayName("Multiple outcomes with same red count should produce same winners")
    void testConsistentWinners() {
        // Both have 2 red, should have same winners
        boolean[] discs1 = {true, true, false, false};
        boolean[] discs2 = {true, false, true, false};

        XocDiaResult result1 = new XocDiaResult(serverSeed, serverSeedHash, discs1);
        XocDiaResult result2 = new XocDiaResult(serverSeed, serverSeedHash, discs2);

        assertEquals(result1.getRedCount(), result2.getRedCount());
        assertEquals(result1.getWinningBets(), result2.getWinningBets());
    }
}
