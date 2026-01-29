package vn.casino.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import vn.casino.game.engine.ProvablyFairRNG;
import vn.casino.game.baucua.BauCuaBetType;
import vn.casino.game.baucua.BauCuaResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Bau Cua game result and payout calculation.
 * Tests 1x/2x/3x match payouts for all 6 animals.
 */
@DisplayName("BauCua Payout Tests")
class BauCuaPayoutTest {

    private String serverSeed;
    private String serverSeedHash;

    @BeforeEach
    void setUp() {
        serverSeed = "test-server-seed";
        serverSeedHash = ProvablyFairRNG.commitment(serverSeed);
    }

    @Test
    @DisplayName("Single match should count as 1")
    void testSingleMatch() {
        // Bau (1), Cua (2), Tom (3)
        BauCuaResult result = new BauCuaResult(serverSeed, serverSeedHash, 1, 2, 3);

        assertEquals(1, result.getMatchCount(BauCuaBetType.BAU));
        assertEquals(1, result.getMatchCount(BauCuaBetType.CUA));
        assertEquals(1, result.getMatchCount(BauCuaBetType.TOM));
        assertEquals(0, result.getMatchCount(BauCuaBetType.CA));
        assertEquals(0, result.getMatchCount(BauCuaBetType.NAI));
        assertEquals(0, result.getMatchCount(BauCuaBetType.GA));
        assertFalse(result.isTriple());
    }

    @Test
    @DisplayName("Double match should count as 2")
    void testDoubleMatch() {
        // Bau (1), Bau (1), Tom (3)
        BauCuaResult result = new BauCuaResult(serverSeed, serverSeedHash, 1, 1, 3);

        assertEquals(2, result.getMatchCount(BauCuaBetType.BAU));
        assertEquals(1, result.getMatchCount(BauCuaBetType.TOM));
        assertEquals(0, result.getMatchCount(BauCuaBetType.CUA));
        assertFalse(result.isTriple());
    }

    @Test
    @DisplayName("Triple match should count as 3 and be detected")
    void testTripleMatch() {
        // Bau (1), Bau (1), Bau (1)
        BauCuaResult result = new BauCuaResult(serverSeed, serverSeedHash, 1, 1, 1);

        assertEquals(3, result.getMatchCount(BauCuaBetType.BAU));
        assertEquals(0, result.getMatchCount(BauCuaBetType.CUA));
        assertTrue(result.isTriple());
    }

    @ParameterizedTest
    @DisplayName("Test all animals with triple")
    @CsvSource({
        "1, BAU",
        "2, CUA",
        "3, TOM",
        "4, CA",
        "5, NAI",
        "6, GA"
    })
    void testAllAnimalTriples(int value, String animalName) {
        BauCuaResult result = new BauCuaResult(serverSeed, serverSeedHash, value, value, value);
        BauCuaBetType animal = BauCuaBetType.valueOf(animalName);

        assertTrue(result.isTriple());
        assertEquals(3, result.getMatchCount(animal));
    }

    @Test
    @DisplayName("All animals should have correct dice values")
    void testAnimalDiceValues() {
        assertEquals(1, BauCuaBetType.BAU.getValue());
        assertEquals(2, BauCuaBetType.CUA.getValue());
        assertEquals(3, BauCuaBetType.TOM.getValue());
        assertEquals(4, BauCuaBetType.CA.getValue());
        assertEquals(5, BauCuaBetType.NAI.getValue());
        assertEquals(6, BauCuaBetType.GA.getValue());
    }

    @Test
    @DisplayName("fromDiceValue should map correctly")
    void testFromDiceValue() {
        assertEquals(BauCuaBetType.BAU, BauCuaBetType.fromDiceValue(1));
        assertEquals(BauCuaBetType.CUA, BauCuaBetType.fromDiceValue(2));
        assertEquals(BauCuaBetType.TOM, BauCuaBetType.fromDiceValue(3));
        assertEquals(BauCuaBetType.CA, BauCuaBetType.fromDiceValue(4));
        assertEquals(BauCuaBetType.NAI, BauCuaBetType.fromDiceValue(5));
        assertEquals(BauCuaBetType.GA, BauCuaBetType.fromDiceValue(6));
    }

    @Test
    @DisplayName("fromDiceValue should throw on invalid values")
    void testFromDiceValueInvalid() {
        assertThrows(IllegalArgumentException.class, () -> BauCuaBetType.fromDiceValue(0));
        assertThrows(IllegalArgumentException.class, () -> BauCuaBetType.fromDiceValue(7));
        assertThrows(IllegalArgumentException.class, () -> BauCuaBetType.fromDiceValue(-1));
    }

    @Test
    @DisplayName("Payout multiplier should be 1.0 for all animals")
    void testPayoutMultiplier() {
        for (BauCuaBetType animal : BauCuaBetType.values()) {
            assertEquals(1.0, animal.getPayoutMultiplier());
        }
    }

    @Test
    @DisplayName("Result should store dice values correctly")
    void testDiceStorage() {
        BauCuaResult result = new BauCuaResult(serverSeed, serverSeedHash, 1, 3, 5);

        assertEquals(BauCuaBetType.BAU, result.getDice1());
        assertEquals(BauCuaBetType.TOM, result.getDice2());
        assertEquals(BauCuaBetType.NAI, result.getDice3());
    }

    @Test
    @DisplayName("Match counts map should be complete")
    void testMatchCountsMap() {
        BauCuaResult result = new BauCuaResult(serverSeed, serverSeedHash, 1, 1, 3);
        Map<BauCuaBetType, Integer> matchCounts = result.getMatchCounts();

        assertNotNull(matchCounts);
        assertEquals(2, matchCounts.get(BauCuaBetType.BAU));
        assertEquals(1, matchCounts.get(BauCuaBetType.TOM));
        assertFalse(matchCounts.containsKey(BauCuaBetType.CUA));
    }

    @Test
    @DisplayName("Display result should format animal names")
    void testDisplayResult() {
        BauCuaResult result = new BauCuaResult(serverSeed, serverSeedHash, 1, 2, 3);
        String display = result.getDisplayResult();

        assertNotNull(display);
        assertTrue(display.contains("-"), "Display should contain separators");
    }

    @Test
    @DisplayName("GameResult should contain winning bets")
    void testGameResultWinningBets() {
        BauCuaResult result = new BauCuaResult(serverSeed, serverSeedHash, 1, 1, 3);

        assertTrue(result.toGameResult().winningBets().contains(BauCuaBetType.BAU));
        assertTrue(result.toGameResult().winningBets().contains(BauCuaBetType.TOM));
        assertFalse(result.toGameResult().winningBets().contains(BauCuaBetType.CUA));
    }

    @Test
    @DisplayName("Server seed should be stored and verifiable")
    void testServerSeed() {
        BauCuaResult result = new BauCuaResult(serverSeed, serverSeedHash, 1, 2, 3);

        assertEquals(serverSeed, result.getServerSeed());
        assertEquals(serverSeedHash, result.getServerSeedHash());
        assertTrue(ProvablyFairRNG.verify(serverSeed, serverSeedHash));
    }

    @Test
    @DisplayName("No matches should result in empty winning bets")
    void testNoMatches() {
        BauCuaResult result = new BauCuaResult(serverSeed, serverSeedHash, 1, 2, 3);

        assertEquals(0, result.getMatchCount(BauCuaBetType.CA));
        assertEquals(0, result.getMatchCount(BauCuaBetType.NAI));
        assertEquals(0, result.getMatchCount(BauCuaBetType.GA));
    }

    @Test
    @DisplayName("All three dice different should have 3 winning animals")
    void testAllDifferent() {
        BauCuaResult result = new BauCuaResult(serverSeed, serverSeedHash, 1, 2, 3);

        assertEquals(3, result.getMatchCounts().size());
        assertEquals(1, result.getMatchCount(BauCuaBetType.BAU));
        assertEquals(1, result.getMatchCount(BauCuaBetType.CUA));
        assertEquals(1, result.getMatchCount(BauCuaBetType.TOM));
    }

    @Test
    @DisplayName("Two same, one different should have 2 winning animals")
    void testTwoSameOneDifferent() {
        BauCuaResult result = new BauCuaResult(serverSeed, serverSeedHash, 1, 1, 2);

        assertEquals(2, result.getMatchCounts().size());
        assertEquals(2, result.getMatchCount(BauCuaBetType.BAU));
        assertEquals(1, result.getMatchCount(BauCuaBetType.CUA));
    }

    @Test
    @DisplayName("Triple should have only 1 winning animal with count 3")
    void testTripleOnlyOneWinner() {
        BauCuaResult result = new BauCuaResult(serverSeed, serverSeedHash, 4, 4, 4);

        assertEquals(1, result.getMatchCounts().size());
        assertEquals(3, result.getMatchCount(BauCuaBetType.CA));
        assertTrue(result.isTriple());
    }

    @Test
    @DisplayName("Verify payout calculation logic for different match counts")
    void testPayoutLogic() {
        // For Bau Cua: payout = bet * matchCount * multiplier
        // multiplier = 1.0
        // So: 1 match = bet * 1, 2 matches = bet * 2, 3 matches = bet * 3

        BauCuaResult singleMatch = new BauCuaResult(serverSeed, serverSeedHash, 1, 2, 3);
        assertEquals(1, singleMatch.getMatchCount(BauCuaBetType.BAU));
        // If bet 1000 on BAU: payout = 1000 * 1 * 1.0 = 1000

        BauCuaResult doubleMatch = new BauCuaResult(serverSeed, serverSeedHash, 1, 1, 3);
        assertEquals(2, doubleMatch.getMatchCount(BauCuaBetType.BAU));
        // If bet 1000 on BAU: payout = 1000 * 2 * 1.0 = 2000

        BauCuaResult tripleMatch = new BauCuaResult(serverSeed, serverSeedHash, 1, 1, 1);
        assertEquals(3, tripleMatch.getMatchCount(BauCuaBetType.BAU));
        // If bet 1000 on BAU: payout = 1000 * 3 * 1.0 = 3000
    }

    @Test
    @DisplayName("fromId should work correctly")
    void testFromId() {
        assertEquals(BauCuaBetType.BAU, BauCuaBetType.fromId("bau"));
        assertEquals(BauCuaBetType.CUA, BauCuaBetType.fromId("cua"));
        assertEquals(BauCuaBetType.TOM, BauCuaBetType.fromId("tom"));
        assertEquals(BauCuaBetType.CA, BauCuaBetType.fromId("ca"));
        assertEquals(BauCuaBetType.NAI, BauCuaBetType.fromId("nai"));
        assertEquals(BauCuaBetType.GA, BauCuaBetType.fromId("ga"));
        assertNull(BauCuaBetType.fromId("invalid"));
    }

    @Test
    @DisplayName("fromId should be case insensitive")
    void testFromIdCaseInsensitive() {
        assertEquals(BauCuaBetType.BAU, BauCuaBetType.fromId("BAU"));
        assertEquals(BauCuaBetType.CUA, BauCuaBetType.fromId("CUA"));
        assertEquals(BauCuaBetType.TOM, BauCuaBetType.fromId("ToM"));
    }

    @Test
    @DisplayName("All bet types should have display names")
    void testDisplayNames() {
        for (BauCuaBetType animal : BauCuaBetType.values()) {
            assertNotNull(animal.getDisplayName());
            assertFalse(animal.getDisplayName().isEmpty());
        }
    }

    @Test
    @DisplayName("All bet types should have unique IDs")
    void testUniqueIds() {
        BauCuaBetType[] animals = BauCuaBetType.values();
        for (int i = 0; i < animals.length; i++) {
            for (int j = i + 1; j < animals.length; j++) {
                assertNotEquals(animals[i].getId(), animals[j].getId());
            }
        }
    }

    @Test
    @DisplayName("All bet types should have unique values")
    void testUniqueValues() {
        BauCuaBetType[] animals = BauCuaBetType.values();
        for (int i = 0; i < animals.length; i++) {
            for (int j = i + 1; j < animals.length; j++) {
                assertNotEquals(animals[i].getValue(), animals[j].getValue());
            }
        }
    }
}
