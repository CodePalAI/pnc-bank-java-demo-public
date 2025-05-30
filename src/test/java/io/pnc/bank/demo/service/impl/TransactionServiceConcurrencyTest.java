package io.pnc.bank.demo.service.impl;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

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

    private Account sourceAccount;
    private Account destinationAccount;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up existing data
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
        sourceAccount = accountRepository.save(sourceAccount);

        destinationAccount = Account.builder()
                .accountNumber("0987654321")
                .accountHolder("Jane Smith")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE)
                .createdDate(LocalDateTime.now())
                .lastModifiedDate(LocalDateTime.now())
                .build();
        destinationAccount = accountRepository.save(destinationAccount);
    }

    @Test
    void testConcurrentTransfersWithOptimisticLocking() throws InterruptedException {
        int numberOfThreads = 10;
        BigDecimal transferAmount = new BigDecimal("10.00");
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        
        AtomicInteger successfulTransfers = new AtomicInteger(0);
        AtomicInteger failedTransfers = new AtomicInteger(0);
        List<Exception> exceptions = new CopyOnWriteArrayList<>();

        // Submit concurrent transfer tasks
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    TransferRequest transferRequest = new TransferRequest();
                    transferRequest.setFromAccountNumber(sourceAccount.getAccountNumber());
                    transferRequest.setToAccountNumber(destinationAccount.getAccountNumber());
                    transferRequest.setAmount(transferAmount);
                    transferRequest.setDescription("Concurrent transfer test " + threadId);

                    transactionService.transfer(transferRequest);
                    successfulTransfers.incrementAndGet();
                    log.info("Thread {} - Transfer completed successfully", threadId);
                } catch (OptimisticLockingFailureException e) {
                    failedTransfers.incrementAndGet();
                    exceptions.add(e);
                    log.warn("Thread {} - Transfer failed due to optimistic locking: {}", threadId, e.getMessage());
                } catch (Exception e) {
                    failedTransfers.incrementAndGet();
                    exceptions.add(e);
                    log.error("Thread {} - Transfer failed with unexpected error: {}", threadId, e.getMessage(), e);
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all threads to complete
        assertTrue(latch.await(30, TimeUnit.SECONDS), "Not all threads completed within timeout");
        executorService.shutdown();

        // Verify results
        log.info("Successful transfers: {}, Failed transfers: {}", successfulTransfers.get(), failedTransfers.get());
        
        // At least some transfers should succeed
        assertTrue(successfulTransfers.get() > 0, "At least some transfers should succeed");
        
        // Total attempts should equal number of threads
        assertEquals(numberOfThreads, successfulTransfers.get() + failedTransfers.get(),
                "Total attempts should equal number of threads");

        // Verify account balances are consistent
        Account updatedSourceAccount = accountRepository.findByAccountNumber(sourceAccount.getAccountNumber()).orElseThrow();
        Account updatedDestinationAccount = accountRepository.findByAccountNumber(destinationAccount.getAccountNumber()).orElseThrow();

        BigDecimal expectedSourceBalance = new BigDecimal("1000.00")
                .subtract(transferAmount.multiply(BigDecimal.valueOf(successfulTransfers.get())));
        BigDecimal expectedDestinationBalance = new BigDecimal("500.00")
                .add(transferAmount.multiply(BigDecimal.valueOf(successfulTransfers.get())));

        assertEquals(expectedSourceBalance, updatedSourceAccount.getBalance(),
                "Source account balance should be consistent");
        assertEquals(expectedDestinationBalance, updatedDestinationAccount.getBalance(),
                "Destination account balance should be consistent");

        // All failed transfers should be due to optimistic locking or legitimate business exceptions
        for (Exception e : exceptions) {
            assertTrue(e instanceof OptimisticLockingFailureException || 
                      e.getMessage().contains("Insufficient balance"),
                      "Failed transfers should only be due to optimistic locking or insufficient balance");
        }
    }

    @Test
    void testConcurrentBidirectionalTransfers() throws InterruptedException {
        int numberOfPairs = 5;
        BigDecimal transferAmount = new BigDecimal("50.00");
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfPairs * 2);
        CountDownLatch latch = new CountDownLatch(numberOfPairs * 2);
        
        AtomicInteger successfulTransfers = new AtomicInteger(0);
        AtomicInteger failedTransfers = new AtomicInteger(0);

        // Submit concurrent bidirectional transfer tasks
        for (int i = 0; i < numberOfPairs; i++) {
            final int pairId = i;
            
            // Transfer from source to destination
            executorService.submit(() -> {
                try {
                    TransferRequest transferRequest = new TransferRequest();
                    transferRequest.setFromAccountNumber(sourceAccount.getAccountNumber());
                    transferRequest.setToAccountNumber(destinationAccount.getAccountNumber());
                    transferRequest.setAmount(transferAmount);
                    transferRequest.setDescription("Bidirectional test A->B " + pairId);

                    transactionService.transfer(transferRequest);
                    successfulTransfers.incrementAndGet();
                    log.info("Pair {} - Transfer A->B completed successfully", pairId);
                } catch (Exception e) {
                    failedTransfers.incrementAndGet();
                    log.warn("Pair {} - Transfer A->B failed: {}", pairId, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
            
            // Transfer from destination to source
            executorService.submit(() -> {
                try {
                    TransferRequest transferRequest = new TransferRequest();
                    transferRequest.setFromAccountNumber(destinationAccount.getAccountNumber());
                    transferRequest.setToAccountNumber(sourceAccount.getAccountNumber());
                    transferRequest.setAmount(transferAmount);
                    transferRequest.setDescription("Bidirectional test B->A " + pairId);

                    transactionService.transfer(transferRequest);
                    successfulTransfers.incrementAndGet();
                    log.info("Pair {} - Transfer B->A completed successfully", pairId);
                } catch (Exception e) {
                    failedTransfers.incrementAndGet();
                    log.warn("Pair {} - Transfer B->A failed: {}", pairId, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all threads to complete
        assertTrue(latch.await(30, TimeUnit.SECONDS), "Not all threads completed within timeout");
        executorService.shutdown();

        // Verify results
        log.info("Successful transfers: {}, Failed transfers: {}", successfulTransfers.get(), failedTransfers.get());
        
        // At least some transfers should succeed
        assertTrue(successfulTransfers.get() > 0, "At least some transfers should succeed");

        // Verify account balances are still reasonable (may not be exact due to bidirectional nature)
        Account updatedSourceAccount = accountRepository.findByAccountNumber(sourceAccount.getAccountNumber()).orElseThrow();
        Account updatedDestinationAccount = accountRepository.findByAccountNumber(destinationAccount.getAccountNumber()).orElseThrow();

        // Total money in both accounts should be conserved
        BigDecimal totalBalance = updatedSourceAccount.getBalance().add(updatedDestinationAccount.getBalance());
        BigDecimal expectedTotalBalance = new BigDecimal("1500.00"); // 1000 + 500
        assertEquals(expectedTotalBalance, totalBalance, "Total money should be conserved");
    }

    @Test
    void testOptimisticLockingWithVersionIncrement() {
        // Get initial version
        Account account = accountRepository.findByAccountNumber(sourceAccount.getAccountNumber()).orElseThrow();
        Long initialVersion = account.getVersion();

        // Perform a transfer
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setFromAccountNumber(sourceAccount.getAccountNumber());
        transferRequest.setToAccountNumber(destinationAccount.getAccountNumber());
        transferRequest.setAmount(new BigDecimal("100.00"));
        transferRequest.setDescription("Version increment test");

        transactionService.transfer(transferRequest);

        // Check that version was incremented
        Account updatedAccount = accountRepository.findByAccountNumber(sourceAccount.getAccountNumber()).orElseThrow();
        assertTrue(updatedAccount.getVersion() > initialVersion, 
                "Version should be incremented after transfer");
    }

    @Test
    void testTransferConsistencyUnderLoad() throws InterruptedException {
        // Create additional accounts for more complex scenarios
        List<Account> accounts = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Account account = Account.builder()
                    .accountNumber("ACC" + String.format("%07d", i))
                    .accountHolder("Test User " + i)
                    .accountType(AccountType.CHECKING)
                    .balance(new BigDecimal("1000.00"))
                    .status(AccountStatus.ACTIVE)
                    .createdDate(LocalDateTime.now())
                    .lastModifiedDate(LocalDateTime.now())
                    .build();
            accounts.add(accountRepository.save(account));
        }

        int numberOfTransfers = 50;
        BigDecimal transferAmount = new BigDecimal("10.00");
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(numberOfTransfers);
        
        AtomicInteger successfulTransfers = new AtomicInteger(0);

        // Submit random transfers between accounts
        for (int i = 0; i < numberOfTransfers; i++) {
            final int transferId = i;
            executorService.submit(() -> {
                try {
                    // Pick random source and destination accounts
                    Account source = accounts.get(transferId % accounts.size());
                    Account destination = accounts.get((transferId + 1) % accounts.size());

                    TransferRequest transferRequest = new TransferRequest();
                    transferRequest.setFromAccountNumber(source.getAccountNumber());
                    transferRequest.setToAccountNumber(destination.getAccountNumber());
                    transferRequest.setAmount(transferAmount);
                    transferRequest.setDescription("Load test transfer " + transferId);

                    transactionService.transfer(transferRequest);
                    successfulTransfers.incrementAndGet();
                } catch (Exception e) {
                    log.warn("Transfer {} failed: {}", transferId, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all transfers to complete
        assertTrue(latch.await(60, TimeUnit.SECONDS), "Not all transfers completed within timeout");
        executorService.shutdown();

        // Verify total money is conserved
        BigDecimal totalBalance = accounts.stream()
                .map(account -> accountRepository.findByAccountNumber(account.getAccountNumber()).orElseThrow())
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expectedTotalBalance = new BigDecimal("5000.00"); // 5 accounts * 1000 each
        assertEquals(expectedTotalBalance, totalBalance, "Total money across all accounts should be conserved");

        log.info("Load test completed. Successful transfers: {} out of {}", 
                successfulTransfers.get(), numberOfTransfers);
    }
}