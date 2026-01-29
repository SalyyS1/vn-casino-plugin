package vn.casino;

import com.tcoded.folialib.FoliaLib;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import vn.casino.core.cache.CacheProvider;
import vn.casino.core.cache.CaffeineFallback;
import vn.casino.core.cache.RedisCacheProvider;
import vn.casino.core.config.ConfigManager;
import vn.casino.core.config.GameConfigLoader;
import vn.casino.core.config.MainConfig;
import vn.casino.core.database.DatabaseProvider;
import vn.casino.core.database.MySQLProvider;
import vn.casino.core.database.SQLiteProvider;
import vn.casino.core.scheduler.FoliaScheduler;
import vn.casino.commands.*;
import vn.casino.economy.CurrencyManager;
import vn.casino.economy.TransactionRepository;
import vn.casino.economy.VaultBridge;
import vn.casino.game.baucua.BauCuaConfig;
import vn.casino.game.baucua.BauCuaGame;
import vn.casino.game.engine.GameSessionManager;
import vn.casino.game.jackpot.JackpotManager;
import vn.casino.game.taixiu.TaiXiuConfig;
import vn.casino.game.taixiu.TaiXiuGame;
import vn.casino.game.xocdia.XocDiaConfig;
import vn.casino.game.xocdia.XocDiaGame;
import vn.casino.game.xocdia.XocDiaRoomManager;
import vn.casino.gui.framework.GuiManager;
import vn.casino.i18n.MessageManager;
import vn.casino.placeholder.CasinoPlaceholders;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.logging.Level;

@Getter
public final class CasinoPlugin extends JavaPlugin {

    @Getter
    private static CasinoPlugin instance;

    private FoliaScheduler scheduler;
    private ConfigManager configManager;
    private MainConfig mainConfig;
    private GameConfigLoader gameConfigLoader;
    private MessageManager messageManager;
    private FoliaLib foliaLib;
    private DatabaseProvider databaseProvider;
    private CacheProvider cacheProvider;
    private TransactionRepository transactionRepository;
    private CurrencyManager currencyManager;
    private VaultBridge vaultBridge;

    // Game systems
    private GameSessionManager sessionManager;
    private JackpotManager jackpotManager;
    private GuiManager guiManager;

    // Games
    private TaiXiuGame taiXiuGame;
    private XocDiaGame xocDiaGame;
    private BauCuaGame bauCuaGame;
    private XocDiaRoomManager roomManager;

    // PlaceholderAPI
    private CasinoPlaceholders placeholders;

    @Override
    public void onEnable() {
        instance = this;

        long startTime = System.currentTimeMillis();
        getLogger().info("Starting VN Casino Plugin v" + getDescription().getVersion());

        if (!initializeCore()) {
            getLogger().severe("Failed to initialize core systems. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!loadConfigurations()) {
            getLogger().severe("Failed to load configurations. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!initializeDatabase()) {
            getLogger().severe("Failed to initialize database. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!initializeCache()) {
            getLogger().severe("Failed to initialize cache. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!initializeManagers()) {
            getLogger().severe("Failed to initialize managers. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!initializeEconomy()) {
            getLogger().severe("Failed to initialize economy system. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!initializeGames()) {
            getLogger().severe("Failed to initialize game systems. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!registerCommands()) {
            getLogger().severe("Failed to register commands. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        registerPlaceholders();

        startBackgroundTasks();

        long loadTime = System.currentTimeMillis() - startTime;
        getLogger().info("VN Casino Plugin enabled successfully in " + loadTime + "ms");

        if (mainConfig.isDebug()) {
            getLogger().info("Debug mode is enabled");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling VN Casino Plugin...");

        // Unregister PlaceholderAPI
        if (placeholders != null) {
            placeholders.unregister();
        }

        // Shutdown GUI manager
        if (guiManager != null) {
            guiManager.shutdown();
        }

        // Shutdown session manager
        if (sessionManager != null) {
            sessionManager.shutdown();
        }

        // Shutdown cache
        if (cacheProvider != null) {
            try {
                cacheProvider.shutdown().join();
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Error shutting down cache", e);
            }
        }

        // Shutdown database
        if (databaseProvider != null) {
            try {
                databaseProvider.shutdown().join();
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Error shutting down database", e);
            }
        }

        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }

        // FoliaLib doesn't need explicit closing

        instance = null;
        getLogger().info("VN Casino Plugin disabled successfully");
    }

    private boolean initializeCore() {
        try {
            foliaLib = new FoliaLib(this);
            scheduler = new FoliaScheduler(foliaLib);
            getLogger().info("Initialized scheduler (Folia support: " + foliaLib.isFolia() + ")");
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize core systems", e);
            return false;
        }
    }

    private boolean loadConfigurations() {
        try {
            saveDefaultConfig();
            configManager = new ConfigManager(this);
            mainConfig = configManager.loadMainConfig();
            gameConfigLoader = new GameConfigLoader(this);

            getLogger().info("Loaded configuration (Language: " + mainConfig.getLanguage() + ")");
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to load configurations", e);
            return false;
        }
    }

    private boolean initializeDatabase() {
        try {
            databaseProvider = createDatabaseProvider();
            databaseProvider.initialize().join();

            getLogger().info("Database initialized (" + databaseProvider.getDatabaseType() + ")");
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize database", e);
            return false;
        }
    }

    private boolean initializeCache() {
        try {
            cacheProvider = createCacheProvider();
            cacheProvider.initialize().join();

            getLogger().info("Cache initialized (" + cacheProvider.getCacheType() + ")");
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize cache", e);
            return false;
        }
    }

    private boolean initializeManagers() {
        try {
            messageManager = new MessageManager(this, mainConfig.getLanguage());

            getLogger().info("Initialized message manager");
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize managers", e);
            return false;
        }
    }

    private boolean initializeEconomy() {
        try {
            // Initialize transaction repository
            transactionRepository = new TransactionRepository(databaseProvider, getLogger());

            // Initialize currency manager
            currencyManager = new CurrencyManager(
                databaseProvider,
                cacheProvider,
                transactionRepository,
                getLogger()
            );

            // Initialize Vault bridge (optional)
            BigDecimal exchangeRate = BigDecimal.valueOf(mainConfig.getVaultExchangeRate());
            vaultBridge = new VaultBridge(this, currencyManager, exchangeRate, getLogger());
            vaultBridge.initialize(); // Soft fail if Vault not available

            getLogger().info("Economy system initialized (VND currency)");
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize economy system", e);
            return false;
        }
    }

    private boolean initializeGames() {
        try {
            // Initialize game session manager
            sessionManager = new GameSessionManager(databaseProvider, scheduler, getLogger());

            // Initialize jackpot manager
            jackpotManager = new JackpotManager(databaseProvider, cacheProvider, currencyManager, getLogger());

            // Load game configurations
            TaiXiuConfig taiXiuConfig = gameConfigLoader.loadTaiXiuConfig();
            XocDiaConfig xocDiaConfig = gameConfigLoader.loadXocDiaConfig();
            BauCuaConfig bauCuaConfig = gameConfigLoader.loadBauCuaConfig();

            // Initialize games
            taiXiuGame = new TaiXiuGame(
                taiXiuConfig,
                currencyManager,
                jackpotManager,
                sessionManager,
                getLogger()
            );

            xocDiaGame = new XocDiaGame(
                currencyManager,
                jackpotManager,
                sessionManager,
                getLogger(),
                xocDiaConfig
            );

            bauCuaGame = new BauCuaGame(
                bauCuaConfig,
                currencyManager,
                jackpotManager,
                sessionManager,
                getLogger()
            );

            // Initialize GUI manager
            guiManager = new GuiManager(this, scheduler, messageManager, transactionRepository);
            guiManager.setGames(taiXiuGame, xocDiaGame, bauCuaGame);

            getLogger().info("Game systems initialized (Tai Xiu, Xoc Dia, Bau Cua)");
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize game systems", e);
            return false;
        }
    }

    private boolean registerCommands() {
        try {
            // Register game commands
            getCommand("taixiu").setExecutor(new TaiXiuCommand(guiManager, taiXiuGame, messageManager));
            getCommand("taixiu").setTabCompleter(new TaiXiuCommand(guiManager, taiXiuGame, messageManager));

            XocDiaCommand xocDiaCommand = new XocDiaCommand(guiManager, xocDiaGame, roomManager, messageManager);
            getCommand("xocdia").setExecutor(xocDiaCommand);
            getCommand("xocdia").setTabCompleter(xocDiaCommand);

            getCommand("baucua").setExecutor(new BauCuaCommand(guiManager, bauCuaGame, messageManager));
            getCommand("baucua").setTabCompleter(new BauCuaCommand(guiManager, bauCuaGame, messageManager));

            // Register admin commands
            CasinoCommand casinoCommand = new CasinoCommand(
                configManager, messageManager, sessionManager, roomManager, jackpotManager
            );
            getCommand("casino").setExecutor(casinoCommand);
            getCommand("casino").setTabCompleter(casinoCommand);

            // Register economy command
            EconomyCommand economyCommand = new EconomyCommand(currencyManager, messageManager);
            getCommand("casinoeco").setExecutor(economyCommand);
            getCommand("casinoeco").setTabCompleter(economyCommand);

            // Register verify command
            getCommand("verify").setExecutor(new VerifyCommand(databaseProvider, messageManager, getLogger()));

            getLogger().info("Commands registered (taixiu, xocdia, baucua, casino, casinoeco, verify)");
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to register commands", e);
            return false;
        }
    }

    private void registerPlaceholders() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                placeholders = new CasinoPlaceholders(this, currencyManager, sessionManager, jackpotManager);
                placeholders.register();
                getLogger().info("PlaceholderAPI expansion registered");
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Failed to register PlaceholderAPI expansion", e);
            }
        } else {
            getLogger().info("PlaceholderAPI not found - placeholders disabled");
        }
    }

    private void startBackgroundTasks() {
        // Daily transaction cleanup task (30-day retention)
        scheduler.runTimer(() -> {
            transactionRepository.deleteOlderThan(Duration.ofDays(30))
                .exceptionally(ex -> {
                    getLogger().log(Level.WARNING, "Transaction cleanup failed", ex);
                    return 0;
                });
        }, 0, 24 * 60 * 60 * 20); // Run daily (20 ticks/second * 60 * 60 * 24)

        getLogger().info("Background tasks started (transaction cleanup: daily)");
    }

    private DatabaseProvider createDatabaseProvider() {
        return switch (mainConfig.getDatabaseType().toLowerCase()) {
            case "mysql", "mariadb" -> new MySQLProvider(mainConfig, getLogger());
            case "sqlite" -> new SQLiteProvider(mainConfig, getLogger());
            default -> {
                getLogger().warning("Unknown database type: " + mainConfig.getDatabaseType() + ", using SQLite");
                yield new SQLiteProvider(mainConfig, getLogger());
            }
        };
    }

    private CacheProvider createCacheProvider() {
        if (mainConfig.isRedisEnabled()) {
            try {
                RedisCacheProvider redis = new RedisCacheProvider(mainConfig, getLogger());
                // Test Redis connection before returning
                redis.initialize().join();
                if (redis.isHealthy()) {
                    return redis;
                }
                getLogger().warning("Redis connection failed, falling back to Caffeine");
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Redis initialization failed, using Caffeine fallback", e);
            }
        }
        return new CaffeineFallback(mainConfig, getLogger());
    }

    public void reload() {
        getLogger().info("Reloading configuration...");

        try {
            reloadConfig();
            mainConfig = configManager.loadMainConfig();
            messageManager.reload(mainConfig.getLanguage());
            gameConfigLoader.reload();

            getLogger().info("Configuration reloaded successfully");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to reload configuration", e);
        }
    }

    public boolean isFoliaServer() {
        return foliaLib != null && foliaLib.isFolia();
    }
}
