package vn.casino.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import vn.casino.CasinoPlugin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bridge between VND casino currency and Vault economy system.
 * Provides optional integration with server economy plugins.
 * Exchange rate: configurable VND to Vault currency ratio.
 */
public class VaultBridge {

    private final CasinoPlugin plugin;
    private final CurrencyManager currencyManager;
    private final Logger logger;
    private final BigDecimal exchangeRate;

    private Economy vaultEconomy;
    private boolean enabled;

    /**
     * Create VaultBridge with configurable exchange rate.
     *
     * @param plugin CasinoPlugin instance
     * @param currencyManager Currency manager for VND operations
     * @param exchangeRate Exchange rate (1 Vault currency = X VND)
     * @param logger Logger instance
     */
    public VaultBridge(
        CasinoPlugin plugin,
        CurrencyManager currencyManager,
        BigDecimal exchangeRate,
        Logger logger
    ) {
        this.plugin = plugin;
        this.currencyManager = currencyManager;
        this.exchangeRate = exchangeRate;
        this.logger = logger;
        this.enabled = false;
    }

    /**
     * Initialize Vault integration if available.
     *
     * @return true if Vault is available and loaded successfully
     */
    public boolean initialize() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            logger.info("Vault not found - economy bridge disabled");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer()
            .getServicesManager()
            .getRegistration(Economy.class);

        if (rsp == null) {
            logger.warning("Vault found but no economy provider registered");
            return false;
        }

        vaultEconomy = rsp.getProvider();
        enabled = true;
        logger.info("Vault economy integration enabled (Exchange rate: 1 " +
            vaultEconomy.currencyNamePlural() + " = " + exchangeRate + " VND)");

        return true;
    }

    /**
     * Check if Vault integration is enabled.
     *
     * @return true if Vault is available
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get the Vault economy instance.
     *
     * @return Economy instance or null if not enabled
     */
    public Economy getVaultEconomy() {
        return vaultEconomy;
    }

    /**
     * Convert Vault currency to VND.
     *
     * @param vaultAmount Amount in Vault currency
     * @return Equivalent VND amount
     */
    public BigDecimal convertToVND(double vaultAmount) {
        return BigDecimal.valueOf(vaultAmount)
            .multiply(exchangeRate)
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Convert VND to Vault currency.
     *
     * @param vndAmount Amount in VND
     * @return Equivalent Vault currency amount
     */
    public double convertToVault(BigDecimal vndAmount) {
        return vndAmount.divide(exchangeRate, 2, RoundingMode.HALF_UP)
            .doubleValue();
    }

    /**
     * Deposit from Vault economy to VND casino balance.
     * Withdraws from player's Vault balance and adds to casino balance.
     *
     * @param playerUuid Player UUID
     * @param vaultAmount Amount in Vault currency to convert
     * @return CompletableFuture with new VND balance, or failed future if insufficient Vault funds
     */
    public CompletableFuture<BigDecimal> depositFromVault(UUID playerUuid, double vaultAmount) {
        if (!enabled) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Vault integration is not enabled")
            );
        }

        if (vaultAmount <= 0) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Amount must be positive")
            );
        }

        return CompletableFuture.supplyAsync(() -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerUuid);

            // Check Vault balance
            if (!vaultEconomy.has(player, vaultAmount)) {
                throw new IllegalStateException("Insufficient Vault balance");
            }

            // Withdraw from Vault
            if (!vaultEconomy.withdrawPlayer(player, vaultAmount).transactionSuccess()) {
                throw new IllegalStateException("Vault withdrawal failed");
            }

            BigDecimal vndAmount = convertToVND(vaultAmount);

            // Deposit to casino balance
            try {
                return currencyManager.deposit(
                    playerUuid,
                    vndAmount,
                    TransactionType.DEPOSIT,
                    null,
                    null,
                    "Vault deposit: " + vaultAmount + " " + vaultEconomy.currencyNamePlural()
                ).join();
            } catch (Exception e) {
                // Rollback Vault withdrawal
                vaultEconomy.depositPlayer(player, vaultAmount);
                throw new RuntimeException("Casino deposit failed, Vault transaction rolled back", e);
            }
        }).exceptionally(ex -> {
            logger.log(Level.SEVERE, "Vault deposit failed for " + playerUuid, ex);
            throw new RuntimeException(ex);
        });
    }

    /**
     * Withdraw from VND casino balance to Vault economy.
     * Withdraws from casino balance and adds to player's Vault balance.
     *
     * @param playerUuid Player UUID
     * @param vndAmount Amount in VND to convert
     * @return CompletableFuture with new VND balance, or failed future if insufficient casino funds
     */
    public CompletableFuture<BigDecimal> withdrawToVault(UUID playerUuid, BigDecimal vndAmount) {
        if (!enabled) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Vault integration is not enabled")
            );
        }

        if (vndAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Amount must be positive")
            );
        }

        return CompletableFuture.supplyAsync(() -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerUuid);
            double vaultAmount = convertToVault(vndAmount);

            // Withdraw from casino balance
            BigDecimal newBalance;
            try {
                newBalance = currencyManager.withdraw(
                    playerUuid,
                    vndAmount,
                    TransactionType.WITHDRAW,
                    null,
                    null,
                    "Vault withdrawal: " + vaultAmount + " " + vaultEconomy.currencyNamePlural()
                ).join();
            } catch (Exception e) {
                throw new IllegalStateException("Insufficient casino balance", e);
            }

            // Deposit to Vault
            if (!vaultEconomy.depositPlayer(player, vaultAmount).transactionSuccess()) {
                // Rollback casino withdrawal
                try {
                    currencyManager.deposit(
                        playerUuid,
                        vndAmount,
                        TransactionType.DEPOSIT,
                        null,
                        null,
                        "Rollback failed Vault withdrawal"
                    ).join();
                } catch (Exception rollbackEx) {
                    logger.log(Level.SEVERE, "Failed to rollback casino withdrawal after Vault deposit failure", rollbackEx);
                }
                throw new RuntimeException("Vault deposit failed");
            }

            return newBalance;
        }).exceptionally(ex -> {
            logger.log(Level.SEVERE, "Vault withdrawal failed for " + playerUuid, ex);
            throw new RuntimeException(ex);
        });
    }

    /**
     * Get player's Vault balance.
     *
     * @param playerUuid Player UUID
     * @return Vault balance or 0 if Vault not enabled
     */
    public double getVaultBalance(UUID playerUuid) {
        if (!enabled) {
            return 0.0;
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUuid);
        return vaultEconomy.getBalance(player);
    }

    /**
     * Get player's Vault balance in VND equivalent.
     *
     * @param playerUuid Player UUID
     * @return VND equivalent of Vault balance
     */
    public BigDecimal getVaultBalanceInVND(UUID playerUuid) {
        return convertToVND(getVaultBalance(playerUuid));
    }

    /**
     * Format Vault currency amount with currency symbol.
     *
     * @param amount Amount to format
     * @return Formatted string (e.g., "$100.00")
     */
    public String formatVault(double amount) {
        if (!enabled) {
            return String.valueOf(amount);
        }
        return vaultEconomy.format(amount);
    }

    /**
     * Get Vault currency name (singular).
     *
     * @return Currency name or "money" if Vault not enabled
     */
    public String getCurrencyName() {
        if (!enabled) {
            return "money";
        }
        return vaultEconomy.currencyNameSingular();
    }

    /**
     * Get Vault currency name (plural).
     *
     * @return Currency name plural or "money" if Vault not enabled
     */
    public String getCurrencyNamePlural() {
        if (!enabled) {
            return "money";
        }
        return vaultEconomy.currencyNamePlural();
    }

    /**
     * Get exchange rate.
     *
     * @return Exchange rate (1 Vault = X VND)
     */
    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }
}
