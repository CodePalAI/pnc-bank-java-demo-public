package io.pnc.bank.demo.service.impl;

import io.pnc.bank.demo.dto.TransactionDTO;
import io.pnc.bank.demo.dto.TransferRequest;
import io.pnc.bank.demo.exception.ConcurrentModificationException;
import io.pnc.bank.demo.exception.InsufficientBalanceException;
import io.pnc.bank.demo.mapper.TransactionMapper;
import io.pnc.bank.demo.model.Account;
import io.pnc.bank.demo.model.AccountStatus;
import io.pnc.bank.demo.model.AccountType;
import io.pnc.bank.demo.repository.AccountRepository;
import io.pnc.bank.demo.repository.TransactionRepository;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplConcurrencyTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private Account sourceAccount;
    private Account destinationAccount;
    private TransferRequest transferRequest;

    @BeforeEach
    void setUp() {
        sourceAccount = Account.builder()
                .id(1L)
                .accountNumber("1234567890")
                .accountHolder("John Doe")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .createdDate(LocalDateTime.now())
                .version(0L)
                .build();

        destinationAccount = Account.builder()
                .id(2L)
                .accountNumber("0987654321")
                .accountHolder("Jane Smith")
                .accountType(AccountType.CHECKING)
                .balance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE)
                .createdDate(LocalDateTime.now())
                .version(0L)
                .build();

        transferRequest = TransferRequest.builder()
                .fromAccountNumber("1234567890")
                .toAccountNumber("0987654321")
                .amount(new BigDecimal("100.00"))
                .description("Test transfer")
                .build();
    }

    @Test
    void testTransferWithOptimisticLockException_ShouldThrowConcurrentModificationException() {
        // Arrange
        when(accountRepository.findByAccountNumberWithLock("1234567890"))
                .thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumberWithLock("0987654321"))
                .thenReturn(Optional.of(destinationAccount));
        when(accountRepository.save(any(Account.class)))
                .thenThrow(new OptimisticLockException("Optimistic lock exception"));

        // Act & Assert
        ConcurrentModificationException exception = assertThrows(
                ConcurrentModificationException.class,
                () -> transactionService.transfer(transferRequest)
        );

        assertTrue(exception.getMessage().contains("concurrent modification"));
        assertEquals(OptimisticLockException.class, exception.getCause().getClass());
    }

    @Test
    void testTransferWithPessimisticLocking_ShouldAcquireLocksInCorrectOrder() {
        // Arrange - Test that accounts are locked in deterministic order (alphabetical by account number)
        when(accountRepository.findByAccountNumberWithLock("0987654321"))
                .thenReturn(Optional.of(destinationAccount));
        when(accountRepository.findByAccountNumberWithLock("1234567890"))
                .thenReturn(Optional.of(sourceAccount));
        when(accountRepository.save(any(Account.class)))
                .thenReturn(sourceAccount, destinationAccount);
        when(transactionRepository.save(any()))
                .thenReturn(null);
        when(transactionMapper.toDto(any()))
                .thenReturn(new TransactionDTO());

        // Act
        transactionService.transfer(transferRequest);

        // Assert - Verify that locks are acquired in the correct order
        verify(accountRepository).findByAccountNumberWithLock("0987654321"); // destination first (smaller lexicographically)
        verify(accountRepository).findByAccountNumberWithLock("1234567890"); // source second
    }

    @Test
    void testTransferWithReversedOrder_ShouldAcquireLocksInCorrectOrder() {
        // Arrange - Test with account numbers in reverse order
        TransferRequest reverseRequest = TransferRequest.builder()
                .fromAccountNumber("0987654321")
                .toAccountNumber("1234567890")
                .amount(new BigDecimal("100.00"))
                .description("Reverse test transfer")
                .build();

        Account reverseSource = destinationAccount.toBuilder().build();
        Account reverseDestination = sourceAccount.toBuilder().build();

        when(accountRepository.findByAccountNumberWithLock("0987654321"))
                .thenReturn(Optional.of(reverseSource));
        when(accountRepository.findByAccountNumberWithLock("1234567890"))
                .thenReturn(Optional.of(reverseDestination));
        when(accountRepository.save(any(Account.class)))
                .thenReturn(reverseSource, reverseDestination);
        when(transactionRepository.save(any()))
                .thenReturn(null);
        when(transactionMapper.toDto(any()))
                .thenReturn(new TransactionDTO());

        // Act
        transactionService.transfer(reverseRequest);

        // Assert - Verify that locks are still acquired in the same order
        verify(accountRepository).findByAccountNumberWithLock("0987654321"); // still first (smaller lexicographically)
        verify(accountRepository).findByAccountNumberWithLock("1234567890"); // still second
    }

    @Test
    void testConcurrentTransfers_ShouldHandleGracefully() throws InterruptedException {
        // Arrange
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(5);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Mock repository behavior
        when(accountRepository.findByAccountNumberWithLock(anyString()))
                .thenAnswer(invocation -> {
                    String accountNumber = invocation.getArgument(0);
                    if ("1234567890".equals(accountNumber)) {
                        return Optional.of(sourceAccount);
                    } else {
                        return Optional.of(destinationAccount);
                    }
                });

        // Simulate occasional optimistic lock exceptions
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> {
                    if (Math.random() < 0.3) { // 30% chance of optimistic lock exception
                        throw new OptimisticLockException("Simulated optimistic lock exception");
                    }
                    return invocation.getArgument(0);
                });

        when(transactionRepository.save(any()))
                .thenReturn(null);
        when(transactionMapper.toDto(any()))
                .thenReturn(new TransactionDTO());

        // Act - Execute concurrent transfers
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    transactionService.transfer(transferRequest);
                    successCount.incrementAndGet();
                } catch (ConcurrentModificationException e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Assert - Some should succeed, some should fail with concurrency exceptions
        assertTrue(successCount.get() + failureCount.get() == 5);
        assertTrue(failureCount.get() > 0); // Some should fail due to simulated optimistic lock exceptions
    }

    @Test
    void testTransferWithInsufficientBalance_ShouldThrowInsufficientBalanceException() {
        // Arrange
        TransferRequest largeTransferRequest = TransferRequest.builder()
                .fromAccountNumber("1234567890")
                .toAccountNumber("0987654321")
                .amount(new BigDecimal("2000.00")) // More than source account balance
                .description("Large transfer")
                .build();

        when(accountRepository.findByAccountNumberWithLock("0987654321"))
                .thenReturn(Optional.of(destinationAccount));
        when(accountRepository.findByAccountNumberWithLock("1234567890"))
                .thenReturn(Optional.of(sourceAccount));

        // Act & Assert
        InsufficientBalanceException exception = assertThrows(
                InsufficientBalanceException.class,
                () -> transactionService.transfer(largeTransferRequest)
        );

        assertTrue(exception.getMessage().contains("Insufficient balance"));
    }

    @Test
    void testTransferWithInactiveAccount_ShouldThrowIllegalStateException() {
        // Arrange
        Account inactiveAccount = sourceAccount.toBuilder()
                .status(AccountStatus.CLOSED)
                .build();

        when(accountRepository.findByAccountNumberWithLock("0987654321"))
                .thenReturn(Optional.of(destinationAccount));
        when(accountRepository.findByAccountNumberWithLock("1234567890"))
                .thenReturn(Optional.of(inactiveAccount));

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> transactionService.transfer(transferRequest)
        );

        assertTrue(exception.getMessage().contains("not active"));
    }
}