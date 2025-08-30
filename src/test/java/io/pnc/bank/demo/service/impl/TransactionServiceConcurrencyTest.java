package io.pnc.bank.demo.service.impl;

import io.pnc.bank.demo.dto.TransferRequest;
import io.pnc.bank.demo.exception.ConcurrentModificationException;
import io.pnc.bank.demo.model.Account;
import io.pnc.bank.demo.model.AccountStatus;
import io.pnc.bank.demo.model.AccountType;
import io.pnc.bank.demo.repository.AccountRepository;
import io.pnc.bank.demo.repository.TransactionRepository;
import io.pnc.bank.demo.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TransactionServiceConcurrencyTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private Account sourceAccount;
    private Account destinationAccount;

    @BeforeEach
    @Transactional
    public void setUp() {
        // Clean up any existing data
        transactionRepository.deleteAll();
        accountRepository.deleteAll();

        // Create test accounts
        sourceAccount = Account.builder()
                .accountNumber("1234567890")
                .accountHolder("John Doe")
                .accountType(AccountType.CHECKING)
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .createdDate(LocalDateTime.now())
                .lastModifiedDate(LocalDateTime.now())
                .build();

        destinationAccount = Account.builder()
                .accountNumber("0987654321")
                .accountHolder("Jane Smith")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE)
                .createdDate(LocalDateTime.now())
                .lastModifiedDate(LocalDateTime.now())
                .build();

        sourceAccount = accountRepository.save(sourceAccount);
        destinationAccount = accountRepository.save(destinationAccount);
    }

    @Test
    public void testConcurrentTransfers_ShouldHandleOptimisticLocking() throws Exception {
        // Number of concurrent transfers to execute
        int numberOfTransfers = 10;
        BigDecimal transferAmount = new BigDecimal("10.00");
        
        ExecutorService executor = Executors.newFixedThreadPool(numberOfTransfers);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Create multiple concurrent transfer requests
        CompletableFuture[] futures = new CompletableFuture[numberOfTransfers];
        
        for (int i = 0; i < numberOfTransfers; i++) {
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    TransferRequest request = new TransferRequest();
                    request.setFromAccountNumber(sourceAccount.getAccountNumber());
                    request.setToAccountNumber(destinationAccount.getAccountNumber());
                    request.setAmount(transferAmount);
                    request.setDescription("Concurrent transfer test");

                    transactionService.transfer(request);
                    successCount.incrementAndGet();
                    
                } catch (ConcurrentModificationException ex) {
                    // Expected behavior - some transfers should fail due to optimistic locking
                    failureCount.incrementAndGet();
                } catch (Exception ex) {
                    // Unexpected error
                    fail("Unexpected exception during concurrent transfer: " + ex.getMessage());
                }
            }, executor);
        }

        // Wait for all transfers to complete
        CompletableFuture.allOf(futures).get();
        executor.shutdown();

        // Verify results
        assertTrue(successCount.get() > 0, "At least some transfers should succeed");
        assertTrue(failureCount.get() > 0, "Some transfers should fail due to optimistic locking");
        assertEquals(numberOfTransfers, successCount.get() + failureCount.get(), 
                "Total attempts should equal successes plus failures");

        // Verify final balances are consistent
        Account finalSourceAccount = accountRepository.findByAccountNumber(sourceAccount.getAccountNumber()).orElseThrow();
        Account finalDestinationAccount = accountRepository.findByAccountNumber(destinationAccount.getAccountNumber()).orElseThrow();

        BigDecimal expectedSourceBalance = new BigDecimal("1000.00")
                .subtract(transferAmount.multiply(BigDecimal.valueOf(successCount.get())));
        BigDecimal expectedDestinationBalance = new BigDecimal("500.00")
                .add(transferAmount.multiply(BigDecimal.valueOf(successCount.get())));

        assertEquals(expectedSourceBalance, finalSourceAccount.getBalance(), 
                "Source account balance should reflect successful transfers only");
        assertEquals(expectedDestinationBalance, finalDestinationAccount.getBalance(), 
                "Destination account balance should reflect successful transfers only");

        // Verify the total money in the system is conserved
        BigDecimal totalMoney = finalSourceAccount.getBalance().add(finalDestinationAccount.getBalance());
        assertEquals(new BigDecimal("1500.00"), totalMoney, 
                "Total money in the system should be conserved");
    }

    @Test
    public void testTransfer_WithDeadlockPrevention() throws Exception {
        // Create additional accounts for deadlock testing
        Account account1 = Account.builder()
                .accountNumber("1111111111")
                .accountHolder("Account 1")
                .accountType(AccountType.CHECKING)
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .createdDate(LocalDateTime.now())
                .lastModifiedDate(LocalDateTime.now())
                .build();

        Account account2 = Account.builder()
                .accountNumber("2222222222")
                .accountHolder("Account 2")
                .accountType(AccountType.CHECKING)
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .createdDate(LocalDateTime.now())
                .lastModifiedDate(LocalDateTime.now())
                .build();

        account1 = accountRepository.save(account1);
        account2 = accountRepository.save(account2);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        AtomicInteger completedTransfers = new AtomicInteger(0);

        // Transfer from account1 to account2
        CompletableFuture<Void> transfer1 = CompletableFuture.runAsync(() -> {
            try {
                TransferRequest request = new TransferRequest();
                request.setFromAccountNumber(account1.getAccountNumber());
                request.setToAccountNumber(account2.getAccountNumber());
                request.setAmount(new BigDecimal("100.00"));
                request.setDescription("Transfer 1->2");

                transactionService.transfer(request);
                completedTransfers.incrementAndGet();
            } catch (ConcurrentModificationException ex) {
                // This is acceptable - optimistic locking worked
                System.out.println("Transfer 1->2 failed due to concurrency: " + ex.getMessage());
            }
        }, executor);

        // Transfer from account2 to account1 (potential deadlock scenario)
        CompletableFuture<Void> transfer2 = CompletableFuture.runAsync(() -> {
            try {
                TransferRequest request = new TransferRequest();
                request.setFromAccountNumber(account2.getAccountNumber());
                request.setToAccountNumber(account1.getAccountNumber());
                request.setAmount(new BigDecimal("50.00"));
                request.setDescription("Transfer 2->1");

                transactionService.transfer(request);
                completedTransfers.incrementAndGet();
            } catch (ConcurrentModificationException ex) {
                // This is acceptable - optimistic locking worked
                System.out.println("Transfer 2->1 failed due to concurrency: " + ex.getMessage());
            }
        }, executor);

        // This should complete without deadlock due to consistent ordering
        CompletableFuture.allOf(transfer1, transfer2).get();
        executor.shutdown();

        // At least one transfer should complete successfully
        assertTrue(completedTransfers.get() > 0, "At least one transfer should complete successfully");
        
        // Verify data consistency
        Account finalAccount1 = accountRepository.findByAccountNumber(account1.getAccountNumber()).orElseThrow();
        Account finalAccount2 = accountRepository.findByAccountNumber(account2.getAccountNumber()).orElseThrow();
        
        BigDecimal totalBalance = finalAccount1.getBalance().add(finalAccount2.getBalance());
        assertEquals(new BigDecimal("2000.00"), totalBalance, "Total balance should be preserved");
    }

    @Test
    public void testTransfer_SingleThreaded_ShouldSucceed() {
        // Verify that non-concurrent transfers work correctly
        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber(sourceAccount.getAccountNumber());
        request.setToAccountNumber(destinationAccount.getAccountNumber());
        request.setAmount(new BigDecimal("100.00"));
        request.setDescription("Single transfer test");

        assertDoesNotThrow(() -> transactionService.transfer(request));

        // Verify balances
        Account finalSourceAccount = accountRepository.findByAccountNumber(sourceAccount.getAccountNumber()).orElseThrow();
        Account finalDestinationAccount = accountRepository.findByAccountNumber(destinationAccount.getAccountNumber()).orElseThrow();

        assertEquals(new BigDecimal("900.00"), finalSourceAccount.getBalance());
        assertEquals(new BigDecimal("600.00"), finalDestinationAccount.getBalance());
    }
}