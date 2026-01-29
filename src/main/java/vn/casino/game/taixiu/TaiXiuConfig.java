package vn.casino.game.taixiu;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Configuration class for Tai Xiu game.
 * Contains all game parameters including timing, bet limits, and jackpot settings.
 */
public class TaiXiuConfig {

    private Duration roundDuration;
    private Duration bettingDuration;
    private BigDecimal minBet;
    private BigDecimal maxBet;
    private double payoutMultiplier;
    private JackpotConfig jackpot;
    private int historyDisplayCount;

    /**
     * Default constructor with standard values.
     */
    public TaiXiuConfig() {
        this.roundDuration = Duration.ofSeconds(60);
        this.bettingDuration = Duration.ofSeconds(50);
        this.minBet = new BigDecimal("1000");
        this.maxBet = new BigDecimal("10000000");
        this.payoutMultiplier = 1.98;
        this.jackpot = new JackpotConfig();
        this.historyDisplayCount = 20;
    }

    // Getters and Setters

    public Duration getRoundDuration() {
        return roundDuration;
    }

    public void setRoundDuration(Duration roundDuration) {
        this.roundDuration = roundDuration;
    }

    public Duration getBettingDuration() {
        return bettingDuration;
    }

    public void setBettingDuration(Duration bettingDuration) {
        this.bettingDuration = bettingDuration;
    }

    public BigDecimal getMinBet() {
        return minBet;
    }

    public void setMinBet(BigDecimal minBet) {
        this.minBet = minBet;
    }

    public BigDecimal getMaxBet() {
        return maxBet;
    }

    public void setMaxBet(BigDecimal maxBet) {
        this.maxBet = maxBet;
    }

    public double getPayoutMultiplier() {
        return payoutMultiplier;
    }

    public void setPayoutMultiplier(double payoutMultiplier) {
        this.payoutMultiplier = payoutMultiplier;
    }

    public JackpotConfig getJackpot() {
        return jackpot;
    }

    public void setJackpot(JackpotConfig jackpot) {
        this.jackpot = jackpot;
    }

    public int getHistoryDisplayCount() {
        return historyDisplayCount;
    }

    public void setHistoryDisplayCount(int historyDisplayCount) {
        this.historyDisplayCount = historyDisplayCount;
    }

    /**
     * Jackpot configuration for Tai Xiu.
     */
    public static class JackpotConfig {
        private double contributionRate;
        private double baseTriggerChance;
        private BigDecimal minJackpot;
        private BigDecimal seedAmount;

        public JackpotConfig() {
            this.contributionRate = 0.002; // 0.2% of each bet
            this.baseTriggerChance = 0.00001; // 0.001% base chance
            this.minJackpot = new BigDecimal("1000000");
            this.seedAmount = new BigDecimal("100000");
        }

        public double getContributionRate() {
            return contributionRate;
        }

        public void setContributionRate(double contributionRate) {
            this.contributionRate = contributionRate;
        }

        public double getBaseTriggerChance() {
            return baseTriggerChance;
        }

        public void setBaseTriggerChance(double baseTriggerChance) {
            this.baseTriggerChance = baseTriggerChance;
        }

        public BigDecimal getMinJackpot() {
            return minJackpot;
        }

        public void setMinJackpot(BigDecimal minJackpot) {
            this.minJackpot = minJackpot;
        }

        public BigDecimal getSeedAmount() {
            return seedAmount;
        }

        public void setSeedAmount(BigDecimal seedAmount) {
            this.seedAmount = seedAmount;
        }
    }
}
