package vn.casino.core.config;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class MainConfig {

    private String language = "vi";
    private boolean debug = false;
    private boolean foliaMode = false;

    private String currencyName = "VND";
    private String currencySymbol = "₫";
    private int currencyDecimals = 0;
    private boolean useVault = true;
    private double vaultExchangeRate = 1000.0; // 1 Vault currency = 1000 VND

    private String databaseType = "sqlite";
    private String sqliteFile = "casino.db";
    private String databaseHost = "localhost";
    private int databasePort = 5432;
    private String databaseName = "vncasino";
    private String databaseUsername = "casino";
    private String databasePassword = "password";

    private int maximumPoolSize = 10;
    private int minimumIdle = 2;
    private long connectionTimeout = 30000;
    private long idleTimeout = 600000;
    private long maxLifetime = 1800000;

    private boolean redisEnabled = false;
    private String redisHost = "localhost";
    private int redisPort = 6379;
    private String redisPassword = "";
    private int redisDatabase = 0;
    private int redisPoolSize = 8;
    private int redisTimeout = 2000;

    private int playerCacheDuration = 300;
    private int maxCachedPlayers = 1000;
    private int gameStateDuration = 60;

    private double minBet = 100;
    private double maxBet = 1000000;
    private double houseEdge = 2.5;
    private double animationSpeed = 1.0;
    private boolean soundsEnabled = true;

    private int animationTickRate = 2;
    private int asyncPoolSize = 4;
    private boolean metricsEnabled = true;

    private int maxConcurrentGames = 1;
    private int gameTimeout = 300;
    private int maxBetsPerMinute = 60;
    private int betCooldown = 1;

    private boolean leaderboardEnabled = true;
    private int leaderboardUpdateInterval = 300;
    private int topPlayers = 10;
    private int statsRetentionDays = 30;

    private boolean vipEnabled = true;
    private double defaultMultiplier = 1.0;
    private Map<String, Double> vipTiers = new HashMap<>();

    private boolean placeholderApiEnabled = true;
    private boolean discordEnabled = false;
    private String discordWebhookUrl = "";
    private double minWinNotify = 100000;

    public MainConfig() {
        vipTiers.put("tier1", 1.1);
        vipTiers.put("tier2", 1.25);
        vipTiers.put("tier3", 1.5);
        vipTiers.put("tier4", 2.0);
    }

    public double getVipMultiplier(String tier) {
        return vipTiers.getOrDefault(tier, defaultMultiplier);
    }

    public boolean isDatabasePostgres() {
        return "postgresql".equalsIgnoreCase(databaseType);
    }

    public boolean isDatabaseMysql() {
        return "mysql".equalsIgnoreCase(databaseType);
    }

    public boolean isDatabaseSqlite() {
        return "sqlite".equalsIgnoreCase(databaseType);
    }

    public String getJdbcUrl() {
        return switch (databaseType.toLowerCase()) {
            case "postgresql" -> String.format("jdbc:postgresql://%s:%d/%s",
                    databaseHost, databasePort, databaseName);
            case "mysql" -> String.format("jdbc:mysql://%s:%d/%s",
                    databaseHost, databasePort, databaseName);
            case "sqlite" -> "jdbc:sqlite:" + sqliteFile;
            default -> throw new IllegalStateException("Unknown database type: " + databaseType);
        };
    }

    public String getDriverClassName() {
        return switch (databaseType.toLowerCase()) {
            case "postgresql" -> "org.postgresql.Driver";
            case "mysql" -> "com.mysql.cj.jdbc.Driver";
            case "sqlite" -> "org.sqlite.JDBC";
            default -> throw new IllegalStateException("Unknown database type: " + databaseType);
        };
    }
}
