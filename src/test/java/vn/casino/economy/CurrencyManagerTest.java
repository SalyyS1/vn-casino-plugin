package vn.casino.economy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import vn.casino.core.cache.CacheProvider;
import vn.casino.core.database.DatabaseProvider;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CurrencyManager.
 * Tests deposit/withdraw operations, concurrent access, and cache behavior.
 */
@DisplayName("CurrencyManager Tests")
class CurrencyManagerTest {

    @Mock
    private DatabaseProvider database;

    @Mock
    private CacheProvider cache;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private Logger logger;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private CurrencyManager currencyManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        currencyManager = new CurrencyManager(database, cache, transactionRepository, logger);
    }

    @Test
    @DisplayName("Get balance should return from cache if available")
    void testGetBalanceFromCache() throws Exception {
        UUID playerId = UUID.randomUUID();
        String cacheKey = String.format("casino:player:%s:balance", playerId);

        when(cache.get(cacheKey))
            .thenReturn(CompletableFuture.completedFuture(Optional.of("5000.00")));

        BigDecimal balance = currencyManager.getBalance(playerId).get();

        assertEquals(new BigDecimal("5000.00"), balance);
        verify(cache).get(cacheKey);
        verifyNoInteractions(database);
    }

    @Test
    @DisplayName("Get balance should fetch from database on cache miss")
    void testGetBalanceFromDatabaseOnCacheMiss() throws Exception {
        UUID playerId = UUID.randomUUID();
        String cacheKey = String.format("casino:player:%s:balance", playerId);

        when(cache.get(cacheKey))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        when(database.queryAsync(anyString(), any(), anyString()))
            .thenReturn(CompletableFuture.completedFuture(new BigDecimal("3000.00")));

        when(cache.set(anyString(), anyString(), any(Duration.class)))
            .thenReturn(CompletableFuture.completedFuture(null));

        BigDecimal balance = currencyManager.getBalance(playerId).get();

        assertEquals(new BigDecimal("3000.00"), balance);
        verify(cache).get(cacheKey);
        verify(database).queryAsync(anyString(), any(), anyString());
    }

    @Test
    @DisplayName("Has balance should return true if sufficient")
    void testHasBalanceSufficient() throws Exception {
        UUID playerId = UUID.randomUUID();

        when(cache.get(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of("5000.00")));

        boolean hasBalance = currencyManager.hasBalance(playerId, new BigDecimal("3000")).get();

        assertTrue(hasBalance);
    }

    @Test
    @DisplayName("Has balance should return false if insufficient")
    void testHasBalanceInsufficient() throws Exception {
        UUID playerId = UUID.randomUUID();

        when(cache.get(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of("5000.00")));

        boolean hasBalance = currencyManager.hasBalance(playerId, new BigDecimal("6000")).get();

        assertFalse(hasBalance);
    }

    @Test
    @DisplayName("Deposit should fail with non-positive amount")
    void testDepositNegativeAmount() {
        UUID playerId = UUID.randomUUID();

        assertThrows(ExecutionException.class, () -> {
            currencyManager.deposit(
                playerId,
                new BigDecimal("-100"),
                TransactionType.GIVE,
                null,
                null
            ).get();
        });

        assertThrows(ExecutionException.class, () -> {
            currencyManager.deposit(
                playerId,
                BigDecimal.ZERO,
                TransactionType.GIVE,
                null,
                null
            ).get();
        });
    }

    @Test
    @DisplayName("Withdraw should fail with non-positive amount")
    void testWithdrawNegativeAmount() {
        UUID playerId = UUID.randomUUID();

        assertThrows(ExecutionException.class, () -> {
            currencyManager.withdraw(
                playerId,
                new BigDecimal("-100"),
                TransactionType.BET,
                null,
                null
            ).get();
        });

        assertThrows(ExecutionException.class, () -> {
            currencyManager.withdraw(
                playerId,
                BigDecimal.ZERO,
                TransactionType.BET,
                null,
                null
            ).get();
        });
    }

    @Test
    @DisplayName("Deposit should add to balance")
    void testDepositSuccess() throws Exception {
        UUID playerId = UUID.randomUUID();
        BigDecimal depositAmount = new BigDecimal("1000");

        when(database.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getBigDecimal("balance")).thenReturn(new BigDecimal("5000"));
        when(preparedStatement.executeUpdate()).thenReturn(1);

        when(cache.delete(anyString())).thenReturn(CompletableFuture.completedFuture(null));

        BigDecimal newBalance = currencyManager.deposit(
            playerId,
            depositAmount,
            TransactionType.GIVE,
            null,
            null
        ).get();

        assertEquals(new BigDecimal("6000"), newBalance);
        verify(connection).commit();
        verify(cache).delete(anyString());
    }

    @Test
    @DisplayName("Withdraw should subtract from balance")
    void testWithdrawSuccess() throws Exception {
        UUID playerId = UUID.randomUUID();
        BigDecimal withdrawAmount = new BigDecimal("1000");

        when(database.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getBigDecimal("balance")).thenReturn(new BigDecimal("5000"));
        when(preparedStatement.executeUpdate()).thenReturn(1);

        when(cache.delete(anyString())).thenReturn(CompletableFuture.completedFuture(null));

        BigDecimal newBalance = currencyManager.withdraw(
            playerId,
            withdrawAmount,
            TransactionType.BET,
            "taixiu",
            123L
        ).get();

        assertEquals(new BigDecimal("4000"), newBalance);
        verify(connection).commit();
        verify(cache).delete(anyString());
    }

    @Test
    @DisplayName("Withdraw should fail on insufficient balance")
    void testWithdrawInsufficientBalance() throws Exception {
        UUID playerId = UUID.randomUUID();
        BigDecimal withdrawAmount = new BigDecimal("6000");

        when(database.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getBigDecimal("balance")).thenReturn(new BigDecimal("5000"));

        assertThrows(ExecutionException.class, () -> {
            currencyManager.withdraw(
                playerId,
                withdrawAmount,
                TransactionType.BET,
                "taixiu",
                123L
            ).get();
        });

        verify(connection).rollback();
    }

    @Test
    @DisplayName("Deposit should create player if not exists")
    void testDepositCreatesNewPlayer() throws Exception {
        UUID playerId = UUID.randomUUID();
        BigDecimal depositAmount = new BigDecimal("1000");

        when(database.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        // First query returns no player
        when(resultSet.next()).thenReturn(false).thenReturn(false);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        when(cache.delete(anyString())).thenReturn(CompletableFuture.completedFuture(null));

        BigDecimal newBalance = currencyManager.deposit(
            playerId,
            depositAmount,
            TransactionType.GIVE,
            null,
            null
        ).get();

        // New player starts at 0, deposit adds 1000
        assertEquals(new BigDecimal("1000"), newBalance);
        verify(connection).commit();
    }

    @Test
    @DisplayName("Cache balance should store in cache")
    void testCacheBalance() {
        UUID playerId = UUID.randomUUID();
        BigDecimal balance = new BigDecimal("5000.50");
        String cacheKey = String.format("casino:player:%s:balance", playerId);

        when(cache.set(eq(cacheKey), eq("5000.50"), any(Duration.class)))
            .thenReturn(CompletableFuture.completedFuture(null));

        currencyManager.cacheBalance(playerId, balance);

        verify(cache).set(eq(cacheKey), eq("5000.50"), any(Duration.class));
    }

    @Test
    @DisplayName("Invalidate cache should delete cache entry")
    void testInvalidateCache() {
        UUID playerId = UUID.randomUUID();
        String cacheKey = String.format("casino:player:%s:balance", playerId);

        when(cache.delete(cacheKey))
            .thenReturn(CompletableFuture.completedFuture(null));

        currencyManager.invalidateCache(playerId);

        verify(cache).delete(cacheKey);
    }

    @Test
    @DisplayName("Cleanup player lock should remove lock")
    void testCleanupPlayerLock() {
        UUID playerId = UUID.randomUUID();

        // Should not throw
        assertDoesNotThrow(() -> currencyManager.cleanupPlayerLock(playerId));
    }

    @Test
    @DisplayName("Get balance should handle invalid cached value")
    void testGetBalanceInvalidCache() throws Exception {
        UUID playerId = UUID.randomUUID();
        String cacheKey = String.format("casino:player:%s:balance", playerId);

        when(cache.get(cacheKey))
            .thenReturn(CompletableFuture.completedFuture(Optional.of("invalid")));

        when(database.queryAsync(anyString(), any(), anyString()))
            .thenReturn(CompletableFuture.completedFuture(new BigDecimal("3000")));

        when(cache.set(anyString(), anyString(), any(Duration.class)))
            .thenReturn(CompletableFuture.completedFuture(null));

        BigDecimal balance = currencyManager.getBalance(playerId).get();

        assertEquals(new BigDecimal("3000"), balance);
        verify(database).queryAsync(anyString(), any(), anyString());
    }

    @Test
    @DisplayName("Get balance should return zero on database error")
    void testGetBalanceDatabaseError() throws Exception {
        UUID playerId = UUID.randomUUID();

        when(cache.get(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        when(database.queryAsync(anyString(), any(), anyString()))
            .thenReturn(CompletableFuture.failedFuture(new SQLException("Database error")));

        BigDecimal balance = currencyManager.getBalance(playerId).get();

        assertEquals(BigDecimal.ZERO, balance);
    }

    @Test
    @DisplayName("Transaction should rollback on error")
    void testTransactionRollbackOnError() throws Exception {
        UUID playerId = UUID.randomUUID();

        when(database.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));

        assertThrows(ExecutionException.class, () -> {
            currencyManager.deposit(
                playerId,
                new BigDecimal("1000"),
                TransactionType.GIVE,
                null,
                null
            ).get();
        });

        verify(connection).rollback();
    }

    @Test
    @DisplayName("Deposit with description should work")
    void testDepositWithDescription() throws Exception {
        UUID playerId = UUID.randomUUID();
        String description = "Test deposit";

        when(database.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getBigDecimal("balance")).thenReturn(new BigDecimal("5000"));
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(cache.delete(anyString())).thenReturn(CompletableFuture.completedFuture(null));

        BigDecimal newBalance = currencyManager.deposit(
            playerId,
            new BigDecimal("1000"),
            TransactionType.GIVE,
            null,
            null,
            description
        ).get();

        assertEquals(new BigDecimal("6000"), newBalance);
    }

    @Test
    @DisplayName("Withdraw with description should work")
    void testWithdrawWithDescription() throws Exception {
        UUID playerId = UUID.randomUUID();
        String description = "Test withdrawal";

        when(database.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getBigDecimal("balance")).thenReturn(new BigDecimal("5000"));
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(cache.delete(anyString())).thenReturn(CompletableFuture.completedFuture(null));

        BigDecimal newBalance = currencyManager.withdraw(
            playerId,
            new BigDecimal("1000"),
            TransactionType.BET,
            "taixiu",
            123L,
            description
        ).get();

        assertEquals(new BigDecimal("4000"), newBalance);
    }
}
