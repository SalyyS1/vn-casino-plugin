package vn.casino.economy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Transaction record representing a single currency operation.
 * Immutable record for audit trail and transaction history.
 *
 * @param id Unique transaction ID (auto-generated)
 * @param uuid Player UUID
 * @param type Transaction type (BET, WIN, DEPOSIT, etc.)
 * @param amount Transaction amount (positive for credits, negative for debits)
 * @param balanceBefore Player balance before transaction
 * @param balanceAfter Player balance after transaction
 * @param game Game identifier (taixiu, xocdia, baucua) or null for non-game transactions
 * @param sessionId Game session ID or null
 * @param description Optional transaction description
 * @param createdAt Transaction timestamp
 */
public record Transaction(
    Long id,
    UUID uuid,
    TransactionType type,
    BigDecimal amount,
    BigDecimal balanceBefore,
    BigDecimal balanceAfter,
    String game,
    Long sessionId,
    String description,
    Instant createdAt
) {
    /**
     * Create a new transaction (without ID for insertion).
     */
    public static Transaction create(
        UUID uuid,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String game,
        Long sessionId,
        String description
    ) {
        return new Transaction(
            null,
            uuid,
            type,
            amount,
            balanceBefore,
            balanceAfter,
            game,
            sessionId,
            description,
            Instant.now()
        );
    }

    /**
     * Validate transaction invariants.
     */
    public Transaction {
        if (uuid == null) {
            throw new IllegalArgumentException("Player UUID cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Transaction type cannot be null");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Transaction amount cannot be null or zero");
        }
        if (balanceBefore == null || balanceBefore.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Balance before cannot be null or negative");
        }
        if (balanceAfter == null || balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Balance after cannot be null or negative");
        }
    }
}
