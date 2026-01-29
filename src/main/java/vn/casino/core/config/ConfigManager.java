package vn.casino.core.config;

import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import vn.casino.CasinoPlugin;

@RequiredArgsConstructor
public class ConfigManager {

    private final CasinoPlugin plugin;

    public MainConfig loadMainConfig() {
        FileConfiguration config = plugin.getConfig();

        MainConfig mainConfig = new MainConfig();

        mainConfig.setLanguage(config.getString("general.language", "vi"));
        mainConfig.setDebug(config.getBoolean("general.debug", false));
        mainConfig.setFoliaMode(config.getBoolean("general.folia-mode", false));

        mainConfig.setCurrencyName(config.getString("currency.name", "VND"));
        mainConfig.setCurrencySymbol(config.getString("currency.symbol", "₫"));
        mainConfig.setCurrencyDecimals(config.getInt("currency.decimals", 0));
        mainConfig.setUseVault(config.getBoolean("currency.use-vault", true));

        mainConfig.setDatabaseType(config.getString("database.type", "sqlite"));
        mainConfig.setSqliteFile(config.getString("database.sqlite-file", "casino.db"));
        mainConfig.setDatabaseHost(config.getString("database.host", "localhost"));
        mainConfig.setDatabasePort(config.getInt("database.port", 5432));
        mainConfig.setDatabaseName(config.getString("database.database", "vncasino"));
        mainConfig.setDatabaseUsername(config.getString("database.username", "casino"));
        mainConfig.setDatabasePassword(config.getString("database.password", "password"));

        ConfigurationSection poolSection = config.getConfigurationSection("database.pool");
        if (poolSection != null) {
            mainConfig.setMaximumPoolSize(poolSection.getInt("maximum-pool-size", 10));
            mainConfig.setMinimumIdle(poolSection.getInt("minimum-idle", 2));
            mainConfig.setConnectionTimeout(poolSection.getLong("connection-timeout", 30000));
            mainConfig.setIdleTimeout(poolSection.getLong("idle-timeout", 600000));
            mainConfig.setMaxLifetime(poolSection.getLong("max-lifetime", 1800000));
        }

        mainConfig.setRedisEnabled(config.getBoolean("redis.enabled", false));
        mainConfig.setRedisHost(config.getString("redis.host", "localhost"));
        mainConfig.setRedisPort(config.getInt("redis.port", 6379));
        mainConfig.setRedisPassword(config.getString("redis.password", ""));
        mainConfig.setRedisDatabase(config.getInt("redis.database", 0));
        mainConfig.setRedisPoolSize(config.getInt("redis.pool-size", 8));
        mainConfig.setRedisTimeout(config.getInt("redis.timeout", 2000));

        mainConfig.setPlayerCacheDuration(config.getInt("cache.player-cache-duration", 300));
        mainConfig.setMaxCachedPlayers(config.getInt("cache.max-cached-players", 1000));
        mainConfig.setGameStateDuration(config.getInt("cache.game-state-duration", 60));

        mainConfig.setMinBet(config.getDouble("games.min-bet", 100));
        mainConfig.setMaxBet(config.getDouble("games.max-bet", 1000000));
        mainConfig.setHouseEdge(config.getDouble("games.house-edge", 2.5));
        mainConfig.setAnimationSpeed(config.getDouble("games.animation-speed", 1.0));
        mainConfig.setSoundsEnabled(config.getBoolean("games.sounds-enabled", true));

        mainConfig.setAnimationTickRate(config.getInt("performance.animation-tick-rate", 2));
        mainConfig.setAsyncPoolSize(config.getInt("performance.async-pool-size", 4));
        mainConfig.setMetricsEnabled(config.getBoolean("performance.metrics-enabled", true));

        mainConfig.setMaxConcurrentGames(config.getInt("security.max-concurrent-games", 1));
        mainConfig.setGameTimeout(config.getInt("security.game-timeout", 300));
        mainConfig.setMaxBetsPerMinute(config.getInt("security.max-bets-per-minute", 60));
        mainConfig.setBetCooldown(config.getInt("security.bet-cooldown", 1));

        mainConfig.setLeaderboardEnabled(config.getBoolean("leaderboard.enabled", true));
        mainConfig.setLeaderboardUpdateInterval(config.getInt("leaderboard.update-interval", 300));
        mainConfig.setTopPlayers(config.getInt("leaderboard.top-players", 10));
        mainConfig.setStatsRetentionDays(config.getInt("leaderboard.stats-retention-days", 30));

        mainConfig.setVipEnabled(config.getBoolean("vip.enabled", true));
        mainConfig.setDefaultMultiplier(config.getDouble("vip.default-multiplier", 1.0));

        ConfigurationSection tiersSection = config.getConfigurationSection("vip.tiers");
        if (tiersSection != null) {
            for (String tier : tiersSection.getKeys(false)) {
                mainConfig.getVipTiers().put(tier, tiersSection.getDouble(tier));
            }
        }

        mainConfig.setPlaceholderApiEnabled(config.getBoolean("integrations.placeholderapi.enabled", true));
        mainConfig.setDiscordEnabled(config.getBoolean("integrations.discord.enabled", false));
        mainConfig.setDiscordWebhookUrl(config.getString("integrations.discord.webhook-url", ""));
        mainConfig.setMinWinNotify(config.getDouble("integrations.discord.min-win-notify", 100000));

        return mainConfig;
    }

    public void saveMainConfig(MainConfig mainConfig) {
        FileConfiguration config = plugin.getConfig();

        config.set("general.language", mainConfig.getLanguage());
        config.set("general.debug", mainConfig.isDebug());
        config.set("general.folia-mode", mainConfig.isFoliaMode());

        config.set("currency.name", mainConfig.getCurrencyName());
        config.set("currency.symbol", mainConfig.getCurrencySymbol());
        config.set("currency.decimals", mainConfig.getCurrencyDecimals());
        config.set("currency.use-vault", mainConfig.isUseVault());

        config.set("database.type", mainConfig.getDatabaseType());
        config.set("database.sqlite-file", mainConfig.getSqliteFile());
        config.set("database.host", mainConfig.getDatabaseHost());
        config.set("database.port", mainConfig.getDatabasePort());
        config.set("database.database", mainConfig.getDatabaseName());
        config.set("database.username", mainConfig.getDatabaseUsername());
        config.set("database.password", mainConfig.getDatabasePassword());

        plugin.saveConfig();
    }

    /**
     * Reload configuration from disk.
     */
    public void reload() {
        plugin.reloadConfig();
    }
}
