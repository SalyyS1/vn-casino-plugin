package vn.casino.game.engine;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Provably Fair Random Number Generator using SHA-256.
 * Implements industry-standard provably fair algorithm for casino games.
 *
 * Algorithm:
 * 1. Server generates server seed before betting starts
 * 2. Server commits hash(server_seed) to players before betting
 * 3. After betting closes, server reveals server seed
 * 4. Result = SHA256(server_seed + ":" + client_seed + ":" + nonce) % max
 * 5. Players can verify: hash(server_seed) == committed_hash
 */
public class ProvablyFairRNG {

    /**
     * Generate a cryptographically secure server seed.
     * Uses UUID + nanoTime for maximum entropy.
     *
     * @return 32+ character hex string
     */
    public static String generateServerSeed() {
        return UUID.randomUUID().toString().replace("-", "") +
               Long.toHexString(System.nanoTime());
    }

    /**
     * Create commitment (hash) of server seed.
     * This is shown to players BEFORE betting starts.
     *
     * @param serverSeed Server seed to commit
     * @return SHA-256 hash of server seed (64 hex characters)
     */
    public static String commitment(String serverSeed) {
        return sha256(serverSeed);
    }

    /**
     * Generate a random number in range [0, max).
     * Uses provably fair algorithm with server seed, client seed, and nonce.
     *
     * @param serverSeed Server-generated seed
     * @param clientSeed Client-provided seed (player UUID for this implementation)
     * @param nonce Incrementing counter for multiple values from same seeds
     * @param max Upper bound (exclusive)
     * @return Random number in range [0, max)
     */
    public static int generateResult(
        String serverSeed,
        String clientSeed,
        long nonce,
        int max
    ) {
        if (max <= 0) {
            throw new IllegalArgumentException("Max must be positive");
        }

        String combined = serverSeed + ":" + clientSeed + ":" + nonce;
        String hash = sha256(combined);

        // Take first 8 characters of hash as hex number
        long value = Long.parseLong(hash.substring(0, 8), 16);

        // Use modulo to get value in range
        return (int) (value % max);
    }

    /**
     * Generate a dice roll (1-6) using provably fair RNG.
     *
     * @param serverSeed Server seed
     * @param clientSeed Client seed
     * @param nonce Nonce for this dice
     * @return Dice value [1, 6]
     */
    public static int rollDice(String serverSeed, String clientSeed, long nonce) {
        return generateResult(serverSeed, clientSeed, nonce, 6) + 1;
    }

    /**
     * Generate multiple dice rolls.
     *
     * @param serverSeed Server seed
     * @param clientSeed Client seed
     * @param count Number of dice to roll
     * @return Array of dice values
     */
    public static int[] rollMultipleDice(String serverSeed, String clientSeed, int count) {
        int[] results = new int[count];
        for (int i = 0; i < count; i++) {
            results[i] = rollDice(serverSeed, clientSeed, i);
        }
        return results;
    }

    /**
     * Verify that a server seed matches its commitment.
     * Players use this to verify game fairness.
     *
     * @param serverSeed Revealed server seed
     * @param commitment Committed hash shown before betting
     * @return true if verification passes
     */
    public static boolean verify(String serverSeed, String commitment) {
        if (serverSeed == null || commitment == null) {
            return false;
        }
        return sha256(serverSeed).equals(commitment);
    }

    /**
     * Calculate SHA-256 hash of input string.
     *
     * @param input Input string
     * @return Hex string of hash (64 characters)
     */
    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Convert byte array to hex string.
     *
     * @param bytes Byte array
     * @return Hex string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
