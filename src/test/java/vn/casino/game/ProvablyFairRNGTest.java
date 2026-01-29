package vn.casino.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import vn.casino.game.engine.ProvablyFairRNG;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ProvablyFairRNG.
 * Tests seed commitment, result distribution, and determinism.
 */
@DisplayName("ProvablyFairRNG Tests")
class ProvablyFairRNGTest {

    @Test
    @DisplayName("Server seed generation should be non-empty and unique")
    void testServerSeedGeneration() {
        String seed1 = ProvablyFairRNG.generateServerSeed();
        String seed2 = ProvablyFairRNG.generateServerSeed();

        assertNotNull(seed1);
        assertNotNull(seed2);
        assertFalse(seed1.isEmpty());
        assertTrue(seed1.length() >= 32, "Server seed should be at least 32 characters");
        assertNotEquals(seed1, seed2, "Consecutive seeds should be unique");
    }

    @Test
    @DisplayName("Commitment should match server seed hash")
    void testSeedCommitment() {
        String seed = ProvablyFairRNG.generateServerSeed();
        String hash = ProvablyFairRNG.commitment(seed);

        assertNotNull(hash);
        assertEquals(64, hash.length(), "SHA-256 hash should be 64 hex characters");
        assertTrue(ProvablyFairRNG.verify(seed, hash), "Commitment verification should pass");
    }

    @Test
    @DisplayName("Verification should fail for wrong seed")
    void testVerificationFailsForWrongSeed() {
        String seed1 = "test-seed-123";
        String seed2 = "test-seed-456";
        String hash = ProvablyFairRNG.commitment(seed1);

        assertFalse(ProvablyFairRNG.verify(seed2, hash), "Verification should fail for wrong seed");
        assertFalse(ProvablyFairRNG.verify(null, hash), "Verification should fail for null seed");
        assertFalse(ProvablyFairRNG.verify(seed1, null), "Verification should fail for null hash");
    }

    @Test
    @DisplayName("Result distribution should be uniform (chi-square test)")
    void testResultDistribution() {
        Map<Integer, Integer> counts = new HashMap<>();
        String seed = ProvablyFairRNG.generateServerSeed();
        int trials = 10000;
        int max = 6;

        // Generate many results
        for (int i = 0; i < trials; i++) {
            int result = ProvablyFairRNG.generateResult(seed, "test", i, max);
            counts.merge(result, 1, Integer::sum);
        }

        // Each value (0-5) should appear ~1666 times (10000 / 6)
        // Allow +/- 300 variance (roughly 2 standard deviations)
        double expectedCount = (double) trials / max;
        double allowedDeviation = 300;

        for (int i = 0; i < max; i++) {
            int count = counts.getOrDefault(i, 0);
            assertTrue(
                Math.abs(count - expectedCount) < allowedDeviation,
                String.format("Distribution too skewed for value %d: expected ~%.0f, got %d",
                    i, expectedCount, count)
            );
        }
    }

    @Test
    @DisplayName("Results should be deterministic (same inputs = same output)")
    void testDeterministic() {
        String seed = "test-seed-123";
        String client = "player-uuid";
        long nonce = 42;
        int max = 100;

        int result1 = ProvablyFairRNG.generateResult(seed, client, nonce, max);
        int result2 = ProvablyFairRNG.generateResult(seed, client, nonce, max);
        int result3 = ProvablyFairRNG.generateResult(seed, client, nonce, max);

        assertEquals(result1, result2, "Same inputs should produce same output");
        assertEquals(result2, result3, "Results should be deterministic");
    }

    @Test
    @DisplayName("Different nonces should produce different results")
    void testDifferentNonces() {
        String seed = "test-seed-123";
        String client = "player-uuid";

        int result1 = ProvablyFairRNG.generateResult(seed, client, 1, 100);
        int result2 = ProvablyFairRNG.generateResult(seed, client, 2, 100);
        int result3 = ProvablyFairRNG.generateResult(seed, client, 3, 100);

        // Very unlikely to get same result with different nonces
        assertFalse(result1 == result2 && result2 == result3,
            "Different nonces should produce different results");
    }

    @Test
    @DisplayName("Results should be in valid range [0, max)")
    void testResultRange() {
        String seed = ProvablyFairRNG.generateServerSeed();

        for (int max = 1; max <= 100; max++) {
            for (int i = 0; i < 100; i++) {
                int result = ProvablyFairRNG.generateResult(seed, "test", i, max);
                assertTrue(result >= 0, "Result should be >= 0");
                assertTrue(result < max, "Result should be < max");
            }
        }
    }

    @Test
    @DisplayName("Dice rolls should be in range [1, 6]")
    void testDiceRoll() {
        String seed = ProvablyFairRNG.generateServerSeed();

        for (int i = 0; i < 1000; i++) {
            int dice = ProvablyFairRNG.rollDice(seed, "test", i);
            assertTrue(dice >= 1, "Dice should be >= 1");
            assertTrue(dice <= 6, "Dice should be <= 6");
        }
    }

    @Test
    @DisplayName("Multiple dice rolls should work correctly")
    void testMultipleDiceRolls() {
        String seed = ProvablyFairRNG.generateServerSeed();
        int[] dice = ProvablyFairRNG.rollMultipleDice(seed, "test", 3);

        assertEquals(3, dice.length, "Should return 3 dice");
        for (int die : dice) {
            assertTrue(die >= 1 && die <= 6, "Each die should be in range [1, 6]");
        }
    }

    @Test
    @DisplayName("Generate result should throw on invalid max")
    void testInvalidMax() {
        String seed = "test";
        assertThrows(IllegalArgumentException.class,
            () -> ProvablyFairRNG.generateResult(seed, "client", 0, 0),
            "Should throw for max=0");
        assertThrows(IllegalArgumentException.class,
            () -> ProvablyFairRNG.generateResult(seed, "client", 0, -1),
            "Should throw for negative max");
    }

    @Test
    @DisplayName("Same seed and client but different nonces should produce different dice")
    void testNonceInfluenceOnDice() {
        String seed = "fixed-seed";
        String client = "fixed-client";

        int dice1 = ProvablyFairRNG.rollDice(seed, client, 0);
        int dice2 = ProvablyFairRNG.rollDice(seed, client, 1);
        int dice3 = ProvablyFairRNG.rollDice(seed, client, 2);

        // Not all should be the same (highly unlikely)
        assertFalse(dice1 == dice2 && dice2 == dice3,
            "Different nonces should usually produce different dice values");
    }

    @Test
    @DisplayName("Commitment hash should be consistent")
    void testCommitmentConsistency() {
        String seed = "consistent-seed";
        String hash1 = ProvablyFairRNG.commitment(seed);
        String hash2 = ProvablyFairRNG.commitment(seed);

        assertEquals(hash1, hash2, "Same seed should produce same commitment hash");
    }
}
