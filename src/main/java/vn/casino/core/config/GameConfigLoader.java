package vn.casino.core.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import vn.casino.CasinoPlugin;
import vn.casino.game.baucua.BauCuaConfig;
import vn.casino.game.jackpot.JackpotConfig;
import vn.casino.game.taixiu.TaiXiuConfig;
import vn.casino.game.xocdia.XocDiaConfig;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

@RequiredArgsConstructor
public class GameConfigLoader {

    private final CasinoPlugin plugin;

    @Getter
    private final Map<String, FileConfiguration> gameConfigs = new HashMap<>();

    public void loadGameConfigs() {
        File gamesDir = new File(plugin.getDataFolder(), "games");
        if (!gamesDir.exists()) {
            gamesDir.mkdirs();
            createDefaultGameConfigs(gamesDir);
        }

        File[] gameFiles = gamesDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (gameFiles == null || gameFiles.length == 0) {
            plugin.getLogger().warning("No game configuration files found in games/ directory");
            return;
        }

        for (File gameFile : gameFiles) {
            try {
                String gameName = gameFile.getName().replace(".yml", "");
                FileConfiguration config = YamlConfiguration.loadConfiguration(gameFile);
                gameConfigs.put(gameName, config);
                plugin.getLogger().info("Loaded game config: " + gameName);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load game config: " + gameFile.getName(), e);
            }
        }
    }

    public void reload() {
        gameConfigs.clear();
        loadGameConfigs();
    }

    public FileConfiguration getGameConfig(String gameName) {
        return gameConfigs.get(gameName);
    }

    public boolean isGameEnabled(String gameName) {
        FileConfiguration config = gameConfigs.get(gameName);
        return config != null && config.getBoolean("enabled", true);
    }

    private void createDefaultGameConfigs(File gamesDir) {
        createTaiXiuConfig(gamesDir);
        createXocDiaConfig(gamesDir);
        createBauCuaConfig(gamesDir);
    }

    private void createTaiXiuConfig(File gamesDir) {
        File taiXiuFile = new File(gamesDir, "tai-xiu.yml");
        FileConfiguration config = new YamlConfiguration();

        config.set("enabled", true);
        config.set("min-bet", 100);
        config.set("max-bet", 100000);
        config.set("animation-duration", 60);
        config.set("payout.tai", 1.95);
        config.set("payout.xiu", 1.95);
        config.set("payout.triple", 30.0);

        try {
            config.save(taiXiuFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create tai-xiu.yml", e);
        }
    }

    private void createXocDiaConfig(File gamesDir) {
        File xocDiaFile = new File(gamesDir, "xoc-dia.yml");
        FileConfiguration config = new YamlConfiguration();

        config.set("enabled", true);
        config.set("min-bet", 100);
        config.set("max-bet", 50000);
        config.set("animation-duration", 80);
        config.set("payout.chan", 1.95);
        config.set("payout.le", 1.95);
        config.set("payout.4-trang", 8.0);
        config.set("payout.4-do", 8.0);
        config.set("payout.3-trang-1-do", 2.5);
        config.set("payout.3-do-1-trang", 2.5);

        try {
            config.save(xocDiaFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create xoc-dia.yml", e);
        }
    }

    private void createBauCuaConfig(File gamesDir) {
        File bauCuaFile = new File(gamesDir, "bau-cua.yml");
        FileConfiguration config = new YamlConfiguration();

        config.set("enabled", true);
        config.set("min-bet", 100);
        config.set("max-bet", 50000);
        config.set("animation-duration", 60);
        config.set("payout.single", 1.0);
        config.set("payout.double", 2.0);
        config.set("payout.triple", 3.0);

        try {
            config.save(bauCuaFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create bau-cua.yml", e);
        }
    }

    public TaiXiuConfig loadTaiXiuConfig() {
        FileConfiguration config = getGameConfig("tai-xiu");
        if (config == null) {
            return new TaiXiuConfig();
        }

        TaiXiuConfig taiXiuConfig = new TaiXiuConfig();
        taiXiuConfig.setRoundDuration(Duration.ofSeconds(config.getInt("animation-duration", 60)));
        taiXiuConfig.setBettingDuration(Duration.ofSeconds(config.getInt("animation-duration", 60) - 10));
        taiXiuConfig.setMinBet(BigDecimal.valueOf(config.getDouble("min-bet", 100)));
        taiXiuConfig.setMaxBet(BigDecimal.valueOf(config.getDouble("max-bet", 100000)));
        taiXiuConfig.setPayoutMultiplier(config.getDouble("payout.tai", 1.95));

        return taiXiuConfig;
    }

    public XocDiaConfig loadXocDiaConfig() {
        FileConfiguration config = getGameConfig("xoc-dia");
        if (config == null) {
            return XocDiaConfig.createDefault();
        }

        XocDiaConfig xocDiaConfig = XocDiaConfig.createDefault();
        xocDiaConfig.setRoundDuration(Duration.ofSeconds(config.getInt("animation-duration", 80)));
        xocDiaConfig.setBettingDuration(Duration.ofSeconds(config.getInt("animation-duration", 80) - 10));

        return xocDiaConfig;
    }

    public BauCuaConfig loadBauCuaConfig() {
        FileConfiguration config = getGameConfig("bau-cua");
        if (config == null) {
            return new BauCuaConfig();
        }

        BauCuaConfig bauCuaConfig = new BauCuaConfig();
        bauCuaConfig.setRoundDuration(Duration.ofSeconds(config.getInt("animation-duration", 60)));
        bauCuaConfig.setBettingDuration(Duration.ofSeconds(config.getInt("animation-duration", 60) - 10));
        bauCuaConfig.setMinBet(BigDecimal.valueOf(config.getDouble("min-bet", 100)));
        bauCuaConfig.setMaxBet(BigDecimal.valueOf(config.getDouble("max-bet", 50000)));
        bauCuaConfig.setPayoutPerMatch(config.getDouble("payout.single", 1.0));

        return bauCuaConfig;
    }
}
