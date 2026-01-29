package vn.casino.game.jackpot;

import java.math.BigDecimal;

/**
 * Jackpot configuration per game.
 * Defines contribution rate, trigger chance, and pool limits.
 *
 * @param contributionRate Percentage of each bet contributed to jackpot (0.002 = 0.2%)
 * @param baseTriggerChance Base chance to trigger jackpot per round (0.00001 = 0.001%)
 * @param triggerScalingRate Additional chance per 1M VND in pool (0.000001 = 0.0001% per 1M)
 * @param minJackpot Minimum jackpot amount required to trigger
 * @param seedAmount Starting amount after jackpot win (reseeds the pool)
 */
public record JackpotConfig(
    double contributionRate,
    double baseTriggerChance,
    double triggerScalingRate,
    BigDecimal minJackpot,
    BigDecimal seedAmount
) {
    /**
     * Default jackpot configuration.
     * - 0.2% contribution from each bet
     * - 0.001% base trigger chance
     * - +0.0001% chance per 1M VND
     * - Min 100,000 VND to trigger
     * - Reseeds with 10,000 VND
     */
    public static JackpotConfig createDefault() {
        return new JackpotConfig(
            0.002,                                  // 0.2% contribution
            0.00001,                                // 0.001% base chance
            0.000001,                               // 0.0001% per 1M VND
            new BigDecimal("100000"),               // 100k VND minimum
            new BigDecimal("10000")                 // 10k VND seed
        );
    }

    /**
     * Validate configuration.
     */
    public JackpotConfig {
        if (contributionRate <= 0 || contributionRate >= 1) {
            throw new IllegalArgumentException("Contribution rate must be between 0 and 1");
        }
        if (baseTriggerChance <= 0 || baseTriggerChance >= 1) {
            throw new IllegalArgumentException("Base trigger chance must be between 0 and 1");
        }
        if (triggerScalingRate < 0 || triggerScalingRate >= 1) {
            throw new IllegalArgumentException("Trigger scaling rate must be between 0 and 1");
        }
        if (minJackpot == null || minJackpot.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Min jackpot must be positive");
        }
        if (seedAmount == null || seedAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Seed amount cannot be negative");
        }
    }

    /**
     * Calculate trigger chance based on current jackpot pool.
     *
     * @param currentPool Current jackpot pool amount
     * @return Trigger probability [0, 1)
     */
    public double calculateTriggerChance(BigDecimal currentPool) {
        // Base chance + (pool / 1,000,000) * scaling rate
        double poolInMillions = currentPool.divide(new BigDecimal("1000000"), 10, java.math.RoundingMode.HALF_UP)
            .doubleValue();

        return baseTriggerChance + (poolInMillions * triggerScalingRate);
    }

    /**
     * Calculate contribution amount from bet.
     *
     * @param betAmount Bet amount
     * @return Contribution amount
     */
    public BigDecimal calculateContribution(BigDecimal betAmount) {
        return betAmount.multiply(BigDecimal.valueOf(contributionRate))
            .setScale(2, java.math.RoundingMode.DOWN);
    }
}
