package vn.casino.game.baucua;

import vn.casino.game.jackpot.JackpotConfig;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Bau Cua game configuration.
 * Controls round timing, bet limits, and jackpot settings.
 *
 * Default settings:
 * - 50 second rounds (40s betting + 10s result)
 * - 1,000 - 5,000,000 VND bet range
 * - 1:1 payout per match
 * - 0.2% jackpot contribution
 */
public class BauCuaConfig {

    private Duration roundDuration;
    private Duration bettingDuration;
    private BigDecimal minBet;
    private BigDecimal maxBet;
    private double payoutPerMatch;
    private JackpotConfig jackpot;

    /**
     * Default constructor with standard values.
     */
    public BauCuaConfig() {
        this.roundDuration = Duration.ofSeconds(50);
        this.bettingDuration = Duration.ofSeconds(40);
        this.minBet = new BigDecimal("1000");
        this.maxBet = new BigDecimal("5000000");
        this.payoutPerMatch = 1.0;
        this.jackpot = new JackpotConfig(
            0.002,                          // 0.2% contribution
            0.00001,                        // 0.001% base trigger chance
            0.000001,                       // 0.0001% scaling per 1M VND
            new BigDecimal("500000"),       // 500k VND minimum jackpot
            new BigDecimal("50000")         // 50k VND seed amount
        );
    }

    /**
     * Get total round duration (betting + result display).
     *
     * @return Round duration
     */
    public Duration getRoundDuration() {
        return roundDuration;
    }

    /**
     * Set round duration.
     *
     * @param roundDuration Duration
     */
    public void setRoundDuration(Duration roundDuration) {
        this.roundDuration = roundDuration;
    }

    /**
     * Get betting phase duration.
     *
     * @return Betting duration
     */
    public Duration getBettingDuration() {
        return bettingDuration;
    }

    /**
     * Set betting duration.
     *
     * @param bettingDuration Duration
     */
    public void setBettingDuration(Duration bettingDuration) {
        this.bettingDuration = bettingDuration;
    }

    /**
     * Get minimum bet amount.
     *
     * @return Min bet in VND
     */
    public BigDecimal getMinBet() {
        return minBet;
    }

    /**
     * Set minimum bet.
     *
     * @param minBet Amount
     */
    public void setMinBet(BigDecimal minBet) {
        this.minBet = minBet;
    }

    /**
     * Get maximum bet amount.
     *
     * @return Max bet in VND
     */
    public BigDecimal getMaxBet() {
        return maxBet;
    }

    /**
     * Set maximum bet.
     *
     * @param maxBet Amount
     */
    public void setMaxBet(BigDecimal maxBet) {
        this.maxBet = maxBet;
    }

    /**
     * Get payout multiplier per matching die.
     * Standard: 1.0 (1:1 payout per match)
     *
     * @return Payout multiplier
     */
    public double getPayoutPerMatch() {
        return payoutPerMatch;
    }

    /**
     * Set payout per match.
     *
     * @param payoutPerMatch Multiplier
     */
    public void setPayoutPerMatch(double payoutPerMatch) {
        this.payoutPerMatch = payoutPerMatch;
    }

    /**
     * Get jackpot configuration.
     *
     * @return Jackpot config
     */
    public JackpotConfig getJackpot() {
        return jackpot;
    }

    /**
     * Set jackpot configuration.
     *
     * @param jackpot Config
     */
    public void setJackpot(JackpotConfig jackpot) {
        this.jackpot = jackpot;
    }

    /**
     * Create default configuration.
     *
     * @return Default config
     */
    public static BauCuaConfig createDefault() {
        return new BauCuaConfig();
    }
}
