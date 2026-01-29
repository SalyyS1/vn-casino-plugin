package vn.casino.economy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import vn.casino.core.database.DatabaseProvider;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransactionRepository.
 * Tests save/find/delete operations for transaction records.
 */
@DisplayName("TransactionRepository Tests")
class TransactionRepositoryTest {

    @Mock
    private DatabaseProvider database;

    @Mock
    private Logger logger;

    private TransactionRepository repository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        repository = new TransactionRepository(database, logger);
    }

    @Test
    @DisplayName("Save should store transaction in database")
    void testSave() throws Exception {
        UUID playerId = UUID.randomUUID();
        Transaction transaction = new Transaction(
            null,
            playerId,
            TransactionType.BET,
            new BigDecimal("1000"),
            new BigDecimal("5000"),
            new BigDecimal("4000"),
            "taixiu",
            123L,
            "Test bet",
            Instant.now()
        );

        when(database.queryAsync(anyString(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(transaction));

        Transaction saved = repository.save(transaction).get();

        assertNotNull(saved);
        verify(database).queryAsync(anyString(), any(), any());
    }

    @Test
    @DisplayName("Find by player should return transactions")
    void testFindByPlayer() throws Exception {
        UUID playerId = UUID.randomUUID();
        List<Transaction> expectedTransactions = List.of(
            new Transaction(
                1L,
                playerId,
                TransactionType.BET,
                new BigDecimal("-1000"),
                new BigDecimal("5000"),
                new BigDecimal("4000"),
                "taixiu",
                123L,
                null,
                Instant.now()
            )
        );

        when(database.queryAsync(anyString(), any(), anyString(), anyInt()))
            .thenReturn(CompletableFuture.completedFuture(expectedTransactions));

        List<Transaction> transactions = repository.findByPlayer(playerId, 10).get();

        assertNotNull(transactions);
        verify(database).queryAsync(anyString(), any(), anyString(), anyInt());
    }

    @Test
    @DisplayName("Find by game should return transactions")
    void testFindByGame() throws Exception {
        String game = "taixiu";
        List<Transaction> expectedTransactions = List.of();

        when(database.queryAsync(anyString(), any(), anyString(), anyInt()))
            .thenReturn(CompletableFuture.completedFuture(expectedTransactions));

        List<Transaction> transactions = repository.findByGame(game, 10).get();

        assertNotNull(transactions);
        verify(database).queryAsync(anyString(), any(), anyString(), anyInt());
    }

    @Test
    @DisplayName("Delete older than should remove old transactions")
    void testDeleteOlderThan() throws Exception {
        Duration duration = Duration.ofDays(30);

        when(database.updateAsync(anyString(), any()))
            .thenReturn(CompletableFuture.completedFuture(50));

        Integer deletedCount = repository.deleteOlderThan(duration).get();

        assertEquals(50, deletedCount);
        verify(database).updateAsync(anyString(), any());
    }

    @Test
    @DisplayName("Count by player should return count")
    void testCountByPlayer() throws Exception {
        UUID playerId = UUID.randomUUID();

        when(database.queryAsync(anyString(), any(), anyString()))
            .thenReturn(CompletableFuture.completedFuture(42));

        Integer count = repository.countByPlayer(playerId).get();

        assertEquals(42, count);
        verify(database).queryAsync(anyString(), any(), anyString());
    }

    @Test
    @DisplayName("Save should handle database errors gracefully")
    void testSaveDatabaseError() throws Exception {
        UUID playerId = UUID.randomUUID();
        Transaction transaction = new Transaction(
            null,
            playerId,
            TransactionType.BET,
            new BigDecimal("1000"),
            new BigDecimal("5000"),
            new BigDecimal("4000"),
            "taixiu",
            123L,
            null,
            Instant.now()
        );

        when(database.queryAsync(anyString(), any(), any()))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("DB Error")));

        Transaction saved = repository.save(transaction).get();

        // Should return original transaction on error
        assertEquals(transaction, saved);
    }

    @Test
    @DisplayName("Find by player should return empty list on error")
    void testFindByPlayerError() throws Exception {
        UUID playerId = UUID.randomUUID();

        when(database.queryAsync(anyString(), any(), anyString(), anyInt()))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("DB Error")));

        List<Transaction> transactions = repository.findByPlayer(playerId, 10).get();

        assertTrue(transactions.isEmpty());
    }

    @Test
    @DisplayName("Find by game should return empty list on error")
    void testFindByGameError() throws Exception {
        String game = "taixiu";

        when(database.queryAsync(anyString(), any(), anyString(), anyInt()))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("DB Error")));

        List<Transaction> transactions = repository.findByGame(game, 10).get();

        assertTrue(transactions.isEmpty());
    }

    @Test
    @DisplayName("Delete older than should return 0 on error")
    void testDeleteOlderThanError() throws Exception {
        Duration duration = Duration.ofDays(30);

        when(database.updateAsync(anyString(), any()))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("DB Error")));

        Integer deletedCount = repository.deleteOlderThan(duration).get();

        assertEquals(0, deletedCount);
    }

    @Test
    @DisplayName("Count by player should return 0 on error")
    void testCountByPlayerError() throws Exception {
        UUID playerId = UUID.randomUUID();

        when(database.queryAsync(anyString(), any(), anyString()))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("DB Error")));

        Integer count = repository.countByPlayer(playerId).get();

        assertEquals(0, count);
    }

    @Test
    @DisplayName("Delete older than with no old transactions should return 0")
    void testDeleteOlderThanNoRecords() throws Exception {
        Duration duration = Duration.ofDays(30);

        when(database.updateAsync(anyString(), any()))
            .thenReturn(CompletableFuture.completedFuture(0));

        Integer deletedCount = repository.deleteOlderThan(duration).get();

        assertEquals(0, deletedCount);
        verify(logger, never()).info(anyString());
    }

    @Test
    @DisplayName("Transaction record should store all fields correctly")
    void testTransactionRecord() {
        UUID playerId = UUID.randomUUID();
        Instant now = Instant.now();

        Transaction transaction = new Transaction(
            123L,
            playerId,
            TransactionType.WIN,
            new BigDecimal("2000"),
            new BigDecimal("3000"),
            new BigDecimal("5000"),
            "xocdia",
            456L,
            "Big win",
            now
        );

        assertEquals(123L, transaction.id());
        assertEquals(playerId, transaction.uuid());
        assertEquals(TransactionType.WIN, transaction.type());
        assertEquals(new BigDecimal("2000"), transaction.amount());
        assertEquals(new BigDecimal("3000"), transaction.balanceBefore());
        assertEquals(new BigDecimal("5000"), transaction.balanceAfter());
        assertEquals("xocdia", transaction.game());
        assertEquals(456L, transaction.sessionId());
        assertEquals("Big win", transaction.description());
        assertEquals(now, transaction.createdAt());
    }

    @Test
    @DisplayName("Find by player should limit results")
    void testFindByPlayerLimit() throws Exception {
        UUID playerId = UUID.randomUUID();
        int limit = 5;

        when(database.queryAsync(anyString(), any(), anyString(), eq(limit)))
            .thenReturn(CompletableFuture.completedFuture(List.of()));

        repository.findByPlayer(playerId, limit).get();

        verify(database).queryAsync(anyString(), any(), anyString(), eq(limit));
    }

    @Test
    @DisplayName("Find by game should limit results")
    void testFindByGameLimit() throws Exception {
        String game = "baucua";
        int limit = 20;

        when(database.queryAsync(anyString(), any(), eq(game), eq(limit)))
            .thenReturn(CompletableFuture.completedFuture(List.of()));

        repository.findByGame(game, limit).get();

        verify(database).queryAsync(anyString(), any(), eq(game), eq(limit));
    }
}
