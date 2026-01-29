package vn.casino.i18n;

public enum MessageKey {
    PREFIX("prefix"),

    GENERAL_NO_PERMISSION("general.no-permission"),
    GENERAL_PLAYER_ONLY("general.player-only"),
    GENERAL_RELOAD_SUCCESS("general.reload-success"),
    GENERAL_RELOAD_FAILED("general.reload-failed"),
    GENERAL_INVALID_COMMAND("general.invalid-command"),
    GENERAL_COMMAND_COOLDOWN("general.command-cooldown"),
    GENERAL_DATABASE_ERROR("general.database-error"),

    // Admin commands
    ADMIN_SET_MULTIPLIER("admin.set-multiplier"),
    ADMIN_VIEW_STATS("admin.view-stats"),
    ADMIN_GIVE("admin.give"),
    ADMIN_TAKE("admin.take"),

    ECONOMY_INSUFFICIENT_FUNDS("economy.insufficient-funds"),
    ECONOMY_BET_TOO_LOW("economy.bet-too-low"),
    ECONOMY_BET_TOO_HIGH("economy.bet-too-high"),
    ECONOMY_WIN("economy.win"),
    ECONOMY_LOSE("economy.lose"),
    ECONOMY_PUSH("economy.push"),
    ECONOMY_BALANCE("economy.balance"),

    GAME_ALREADY_PLAYING("game.already-playing"),
    GAME_IN_PROGRESS("game.game-in-progress"),
    GAME_NOT_FOUND("game.game-not-found"),
    GAME_CANCELLED("game.game-cancelled"),
    GAME_TIMEOUT("game.game-timeout"),
    GAME_DISABLED("game.game-disabled"),

    SLOTS_TITLE("slots.title"),
    SLOTS_SPINNING("slots.spinning"),
    SLOTS_RESULT("slots.result"),
    SLOTS_JACKPOT("slots.jackpot"),
    SLOTS_MULTIPLIER("slots.multiplier"),

    BLACKJACK_TITLE("blackjack.title"),
    BLACKJACK_YOUR_HAND("blackjack.your-hand"),
    BLACKJACK_DEALER_HAND("blackjack.dealer-hand"),
    BLACKJACK_HIDDEN_CARD("blackjack.hidden-card"),
    BLACKJACK_HIT("blackjack.hit"),
    BLACKJACK_STAND("blackjack.stand"),
    BLACKJACK_DOUBLE("blackjack.double"),
    BLACKJACK_BLACKJACK("blackjack.blackjack"),
    BLACKJACK_BUST("blackjack.bust"),
    BLACKJACK_CHOOSE_ACTION("blackjack.choose-action"),

    ROULETTE_TITLE("roulette.title"),
    ROULETTE_SPINNING("roulette.spinning"),
    ROULETTE_RESULT("roulette.result"),
    ROULETTE_PLACE_BET("roulette.place-bet"),
    ROULETTE_BET_RED("roulette.bet-types.red"),
    ROULETTE_BET_BLACK("roulette.bet-types.black"),
    ROULETTE_BET_EVEN("roulette.bet-types.even"),
    ROULETTE_BET_ODD("roulette.bet-types.odd"),
    ROULETTE_BET_LOW("roulette.bet-types.low"),
    ROULETTE_BET_HIGH("roulette.bet-types.high"),
    ROULETTE_BET_NUMBER("roulette.bet-types.number"),

    BACCARAT_TITLE("baccarat.title"),
    BACCARAT_PLAYER_HAND("baccarat.player-hand"),
    BACCARAT_BANKER_HAND("baccarat.banker-hand"),
    BACCARAT_BET_ON("baccarat.bet-on"),
    BACCARAT_SIDE_PLAYER("baccarat.sides.player"),
    BACCARAT_SIDE_BANKER("baccarat.sides.banker"),
    BACCARAT_SIDE_TIE("baccarat.sides.tie"),
    BACCARAT_NATURAL("baccarat.natural"),

    STATS_TITLE("stats.title"),
    STATS_TOTAL_BETS("stats.personal.total-bets"),
    STATS_TOTAL_WINS("stats.personal.total-wins"),
    STATS_TOTAL_LOSSES("stats.personal.total-losses"),
    STATS_NET_PROFIT("stats.personal.net-profit"),
    STATS_GAMES_PLAYED("stats.personal.games-played"),
    STATS_WIN_RATE("stats.personal.win-rate"),
    STATS_BIGGEST_WIN("stats.personal.biggest-win"),
    STATS_LEADERBOARD_TITLE("stats.leaderboard.title"),
    STATS_LEADERBOARD_ENTRY("stats.leaderboard.entry"),

    HELP_HEADER("help.header"),
    HELP_FOOTER("help.footer");

    private final String key;

    MessageKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        return key;
    }
}
