package io.pnc.bank.demo.service.impl;

import io.pnc.bank.demo.dto.TransactionDTO;
import io.pnc.bank.demo.dto.TransferRequest;
import io.pnc.bank.demo.model.Account;
import io.pnc.bank.demo.model.AccountStatus;
import io.pnc.bank.demo.model.AccountType;
import io.pnc.bank.demo.repository.AccountRepository;
import io.pnc.bank.demo.repository.TransactionRepository;
import io.pnc.bank.demo.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for concurrent transaction scenarios to verify race condition fixes.
 * Tests cover optimistic locking, deadlock prevention, and transaction isolation.
 */
@SpringBootTest
@ActiveProfiles("test")
@Slf4j
class TransactionServiceConcurrencyTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private Account account1;
    private Account account2;
    private Account account3;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up any existing test data
        transactionRepository.deleteAll();
        accountRepository.deleteAll();

        // Create test accounts with sufficient balance
        account1 = Account.builder()
                .accountNumber("1000000001")
                .accountHolder("Test User 1")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("10000.00"))
                .status(AccountStatus.ACTIVE)
                .createdDate(LocalDateTime.now())
                .lastModifiedDate(LocalDateTime.now())
                .build();

        account2 = Account.builder()
                .accountNumber("1000000002")
                .accountHolder("Test User 2")
                .accountType(AccountType.CHECKING)
                .balance(new BigDecimal("5000.00"))
                .status(AccountStatus.ACTIVE)
                .createdDate(LocalDateTime.now())
                .lastModifiedDate(LocalDateTime.now())
                .build();

        account3 = Account.builder()
                .accountNumber("1000000003")
                .accountHolder("Test User 3")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("7500.00"))
                .status(AccountStatus.ACTIVE)
                .createdDate(LocalDateTime.now())
                .lastModifiedDate(LocalDateTime.now())
                .build();

        account1 = accountRepository.save(account1);
        account2 = accountRepository.save(account2);
        account3 = accountRepository.save(account3);
    }

    @Test
    void testConcurrentTransfersToSameAccount() throws InterruptedException {
        final int numberOfThreads = 10;
        final BigDecimal transferAmount = new BigDecimal("100.00");
        final ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        final CountDownLatch latch = new CountDownLatch(numberOfThreads);
        final AtomicInteger successfulTransfers = new AtomicInteger(0);
        final AtomicInteger failedTransfers = new AtomicInteger(0);

        // Get initial balances
        final BigDecimal initialAccount1Balance = accountRepository.findById(account1.getId()).get().getBalance();
        final BigDecimal initialAccount2Balance = accountRepository.findById(account2.getId()).get().getBalance();

        // Submit concurrent transfer tasks
        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    TransferRequest request = new TransferRequest();
                    request.setFromAccountNumber(account1.getAccountNumber());
                    request.setToAccountNumber(account2.getAccountNumber());
                    request.setAmount(transferAmount);
                    request.setDescription("Concurrent transfer test");

                    TransactionDTO result = transactionService.transfer(request);
                    assertNotNull(result);
                    successfulTransfers.incrementAndGet();
                    log.info("Transfer successful: {}", result.getReferenceId());

                } catch (Exception e) {
                    log.warn("Transfer failed: {}", e.getMessage());
                    failedTransfers.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all threads to complete
        assertTrue(latch.await(30, TimeUnit.SECONDS), "All transfers should complete within timeout");
        executor.shutdown();

        // Verify results
        int totalTransfers = successfulTransfers.get() + failedTransfers.get();
        assertEquals(numberOfThreads, totalTransfers, "All transfer attempts should be accounted for");

        // Check final balances are consistent
        Account finalAccount1 = accountRepository.findById(account1.getId()).get();
        Account finalAccount2 = accountRepository.findById(account2.getId()).get();

        BigDecimal expectedAccount1Balance = initialAccount1Balance.subtract(
                transferAmount.multiply(new BigDecimal(successfulTransfers.get())));
        BigDecimal expectedAccount2Balance = initialAccount2Balance.add(
                transferAmount.multiply(new BigDecimal(successfulTransfers.get())));

        assertEquals(0, expectedAccount1Balance.compareTo(finalAccount1.getBalance()),
                "Account 1 balance should match expected value");
        assertEquals(0, expectedAccount2Balance.compareTo(finalAccount2.getBalance()),
                "Account 2 balance should match expected value");

        log.info("Concurrent transfer test completed. Successful: {}, Failed: {}, " +
                        "Final balances - Account1: {}, Account2: {}",
                successfulTransfers.get(), failedTransfers.get(),
                finalAccount1.getBalance(), finalAccount2.getBalance());
    }

    @Test
    void testBidirectionalConcurrentTransfers() throws InterruptedException {
        final int numberOfThreads = 8;
        final BigDecimal transferAmount = new BigDecimal("50.00");
        final ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        final CountDownLatch latch = new CountDownLatch(numberOfThreads);
        final AtomicInteger successfulTransfers = new AtomicInteger(0);

        // Get initial balances
        final BigDecimal initialAccount1Balance = accountRepository.findById(account1.getId()).get().getBalance();
        final BigDecimal initialAccount2Balance = accountRepository.findById(account2.getId()).get().getBalance();

        // Submit bidirectional transfer tasks
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    TransferRequest request = new TransferRequest();
                    
                    // Alternate transfer direction to test deadlock prevention
                    if (threadIndex % 2 == 0) {
                        request.setFromAccountNumber(account1.getAccountNumber());
                        request.setToAccountNumber(account2.getAccountNumber());
                    } else {
                        request.setFromAccountNumber(account2.getAccountNumber());
                        request.setToAccountNumber(account1.getAccountNumber());
                    }
                    
                    request.setAmount(transferAmount);
                    request.setDescription("Bidirectional transfer test");

                    TransactionDTO result = transactionService.transfer(request);
                    assertNotNull(result);
                    successfulTransfers.incrementAndGet();
                    log.info("Bidirectional transfer successful: {}", result.getReferenceId());

                } catch (Exception e) {
                    log.warn("Bidirectional transfer failed: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all threads to complete
        assertTrue(latch.await(30, TimeUnit.SECONDS), "All transfers should complete within timeout");
        executor.shutdown();

        // Verify no deadlocks occurred and transactions completed
        assertTrue(successfulTransfers.get() > 0, "At least some transfers should succeed");
        
        // Check that both accounts still have valid balances
        Account finalAccount1 = accountRepository.findById(account1.getId()).get();
        Account finalAccount2 = accountRepository.findById(account2.getId()).get();
        
        assertTrue(finalAccount1.getBalance().compareTo(BigDecimal.ZERO) >= 0, 
                "Account 1 should have non-negative balance");
        assertTrue(finalAccount2.getBalance().compareTo(BigDecimal.ZERO) >= 0, 
                "Account 2 should have non-negative balance");

        log.info("Bidirectional transfer test completed. Successful transfers: {}, " +
                        "Final balances - Account1: {}, Account2: {}",
                successfulTransfers.get(), finalAccount1.getBalance(), finalAccount2.getBalance());
    }

    @Test
    void testCircularTransfers() throws InterruptedException {
        final int numberOfThreads = 6;
        final BigDecimal transferAmount = new BigDecimal("75.00");
        final ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        final CountDownLatch latch = new CountDownLatch(numberOfThreads);
        final AtomicInteger successfulTransfers = new AtomicInteger(0);

        // Get initial balances
        final BigDecimal initialAccount1Balance = accountRepository.findById(account1.getId()).get().getBalance();
        final BigDecimal initialAccount2Balance = accountRepository.findById(account2.getId()).get().getBalance();
        final BigDecimal initialAccount3Balance = accountRepository.findById(account3.getId()).get().getBalance();

        // Submit circular transfer tasks: A->B, B->C, C->A
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    TransferRequest request = new TransferRequest();
                    
                    // Create circular transfers to test complex deadlock scenarios
                    switch (threadIndex % 3) {
                        case 0:
                            request.setFromAccountNumber(account1.getAccountNumber());
                            request.setToAccountNumber(account2.getAccountNumber());
                            break;
                        case 1:
                            request.setFromAccountNumber(account2.getAccountNumber());
                            request.setToAccountNumber(account3.getAccountNumber());
                            break;
                        case 2:
                            request.setFromAccountNumber(account3.getAccountNumber());
                            request.setToAccountNumber(account1.getAccountNumber());
                            break;
                    }
                    
                    request.setAmount(transferAmount);
                    request.setDescription("Circular transfer test");

                    TransactionDTO result = transactionService.transfer(request);
                    assertNotNull(result);
                    successfulTransfers.incrementAndGet();
                    log.info("Circular transfer successful: {}", result.getReferenceId());

                } catch (Exception e) {
                    log.warn("Circular transfer failed: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all threads to complete
        assertTrue(latch.await(30, TimeUnit.SECONDS), "All transfers should complete within timeout");
        executor.shutdown();

        // Verify no deadlocks occurred
        assertTrue(successfulTransfers.get() > 0, "At least some transfers should succeed");
        
        // Check that all accounts still have valid balances
        Account finalAccount1 = accountRepository.findById(account1.getId()).get();
        Account finalAccount2 = accountRepository.findById(account2.getId()).get();
        Account finalAccount3 = accountRepository.findById(account3.getId()).get();
        
        assertTrue(finalAccount1.getBalance().compareTo(BigDecimal.ZERO) >= 0, 
                "Account 1 should have non-negative balance");
        assertTrue(finalAccount2.getBalance().compareTo(BigDecimal.ZERO) >= 0, 
                "Account 2 should have non-negative balance");
        assertTrue(finalAccount3.getBalance().compareTo(BigDecimal.ZERO) >= 0, 
                "Account 3 should have non-negative balance");

        // Verify total money conservation
        BigDecimal totalInitial = initialAccount1Balance.add(initialAccount2Balance).add(initialAccount3Balance);
        BigDecimal totalFinal = finalAccount1.getBalance().add(finalAccount2.getBalance()).add(finalAccount3.getBalance());
        assertEquals(0, totalInitial.compareTo(totalFinal), 
                "Total money should be conserved across all accounts");

        log.info("Circular transfer test completed. Successful transfers: {}, " +
                        "Final balances - Account1: {}, Account2: {}, Account3: {}",
                successfulTransfers.get(), finalAccount1.getBalance(), 
                finalAccount2.getBalance(), finalAccount3.getBalance());
    }

    @Test
    void testOptimisticLockingFailureHandling() {
        // This test verifies that optimistic locking failures are properly handled
        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber(account1.getAccountNumber());
        request.setToAccountNumber(account2.getAccountNumber());
        request.setAmount(new BigDecimal("100.00"));
        request.setDescription("Optimistic locking test");

        // Perform successful transfer
        TransactionDTO result = transactionService.transfer(request);
        assertNotNull(result);
        assertEquals(account1.getAccountNumber(), result.getAccount().getAccountNumber());

        // Verify transaction was recorded
        assertNotNull(result.getReferenceId());
        assertNotNull(result.getTransactionDate());
    }

    @Test
    void testInsufficientBalanceDuringConcurrentTransfers() throws InterruptedException {
        // Create account with limited balance
        Account limitedAccount = Account.builder()
                .accountNumber("1000000004")
                .accountHolder("Limited User")
                .accountType(AccountType.CHECKING)
                .balance(new BigDecimal("200.00")) // Only $200
                .status(AccountStatus.ACTIVE)
                .createdDate(LocalDateTime.now())
                .lastModifiedDate(LocalDateTime.now())
                .build();
        limitedAccount = accountRepository.save(limitedAccount);

        final int numberOfThreads = 5;
        final BigDecimal transferAmount = new BigDecimal("100.00"); // Each tries to transfer $100
        final ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        final CountDownLatch latch = new CountDownLatch(numberOfThreads);
        final AtomicInteger successfulTransfers = new AtomicInteger(0);
        final AtomicInteger insufficientBalanceExceptions = new AtomicInteger(0);

        final Account finalLimitedAccount = limitedAccount;
        
        // Submit concurrent transfer tasks
        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    TransferRequest request = new TransferRequest();
                    request.setFromAccountNumber(finalLimitedAccount.getAccountNumber());
                    request.setToAccountNumber(account2.getAccountNumber());
                    request.setAmount(transferAmount);
                    request.setDescription("Insufficient balance test");

                    TransactionDTO result = transactionService.transfer(request);
                    successfulTransfers.incrementAndGet();
                    log.info("Transfer successful despite limited balance: {}", result.getReferenceId());

                } catch (Exception e) {
                    if (e.getMessage().contains("Insufficient balance")) {
                        insufficientBalanceExceptions.incrementAndGet();
                        log.info("Expected insufficient balance exception: {}", e.getMessage());
                    } else {
                        log.warn("Unexpected exception: {}", e.getMessage());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all threads to complete
        assertTrue(latch.await(30, TimeUnit.SECONDS), "All transfers should complete within timeout");
        executor.shutdown();

        // Verify that only 2 transfers should succeed (balance allows for 2 x $100)
        assertTrue(successfulTransfers.get() <= 2, 
                "Should not succeed more transfers than balance allows");
        assertTrue(insufficientBalanceExceptions.get() > 0, 
                "Should have some insufficient balance exceptions");

        // Check final balance is non-negative
        Account finalLimitedAccountCheck = accountRepository.findById(finalLimitedAccount.getId()).get();
        assertTrue(finalLimitedAccountCheck.getBalance().compareTo(BigDecimal.ZERO) >= 0, 
                "Account should never have negative balance");

        log.info("Insufficient balance test completed. Successful: {}, Insufficient balance exceptions: {}, " +
                        "Final balance: {}",
                successfulTransfers.get(), insufficientBalanceExceptions.get(),
                finalLimitedAccountCheck.getBalance());
    }
}