package vn.casino.economy;

/**
 * Transaction type enum for all currency operations.
 * Matches database ENUM values in casino_transactions table.
 */
public enum TransactionType {
    /**
     * Player deposit from external source (Vault, admin command).
     */
    DEPOSIT,

    /**
     * Player withdrawal to external source (Vault, admin command).
     */
    WITHDRAW,

    /**
     * Player places a bet in a game.
     */
    BET,

    /**
     * Player wins a bet.
     */
    WIN,

    /**
     * Bet refund (game cancelled, error).
     */
    REFUND,

    /**
     * Jackpot win.
     */
    JACKPOT,

    /**
     * Admin gives money to player.
     */
    ADMIN_GIVE,

    /**
     * Admin takes money from player.
     */
    ADMIN_TAKE,

    /**
     * Admin gives money to player (alias for ADMIN_GIVE).
     */
    GIVE,

    /**
     * Admin takes money from player (alias for ADMIN_TAKE).
     */
    TAKE
}
