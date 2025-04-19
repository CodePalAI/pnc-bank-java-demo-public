package io.pnc.bank.demo.config;

import io.pnc.bank.demo.model.Account;
import io.pnc.bank.demo.model.AccountStatus;
import io.pnc.bank.demo.model.AccountType;
import io.pnc.bank.demo.model.Transaction;
import io.pnc.bank.demo.model.TransactionType;
import io.pnc.bank.demo.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
@Profile("demo")
@RequiredArgsConstructor
@Slf4j
public class DemoDataInitializer implements CommandLineRunner {

    private final AccountRepository accountRepository;

    @Value("${demo.data.initialize:true}")
    private boolean shouldInitialize;

    @Value("${demo.account.count:10}")
    private int accountCount;

    @Value("${demo.transactions.per-account:5}")
    private int transactionsPerAccount;

    private final Random random = new Random();

    @Override
    @Transactional
    public void run(String... args) {
        if (!shouldInitialize) {
            log.info("Demo data initialization is disabled");
            return;
        }

        // Check if we already have accounts
        long existingAccounts = accountRepository.count();
        if (existingAccounts > 0) {
            log.info("Database already contains {} accounts, skipping demo data initialization", existingAccounts);
            return;
        }

        log.info("Initializing demo data with {} accounts and approximately {} transactions per account", 
                accountCount, transactionsPerAccount);

        // Generate accounts
        for (int i = 0; i < accountCount; i++) {
            createAccount(i);
        }

        log.info("Demo data initialization completed successfully");
    }

    private void createAccount(int index) {
        // Generate account number
        String accountNumber = String.format("%010d", 1000000000 + index);

        // Select random account type
        AccountType accountType = getRandomAccountType();

        // Generate a random initial balance between $1000 and $10000
        BigDecimal initialBalance = BigDecimal.valueOf(1000 + random.nextInt(9000));

        // Create account
        Account account = Account.builder()
                .accountNumber(accountNumber)
                .accountHolder(getRandomCustomerName())
                .accountType(accountType)
                .balance(initialBalance)
                .status(AccountStatus.ACTIVE)
                .createdDate(getRandomDateInPast(90)) // Date within last 90 days
                .lastModifiedDate(LocalDateTime.now())
                .build();

        // Save account
        account = accountRepository.save(account);

        // Create initial deposit transaction
        Transaction initialDeposit = Transaction.builder()
                .account(account)
                .amount(initialBalance)
                .transactionType(TransactionType.DEPOSIT)
                .description("Initial deposit")
                .transactionDate(account.getCreatedDate())
                .referenceId(UUID.randomUUID().toString())
                .balanceAfterTransaction(initialBalance)
                .build();

        account.addTransaction(initialDeposit);

        // Create random transactions
        BigDecimal currentBalance = initialBalance;
        LocalDateTime lastTransactionDate = account.getCreatedDate();

        for (int i = 0; i < transactionsPerAccount; i++) {
            TransactionType type = getRandomTransactionType();
            BigDecimal amount = BigDecimal.valueOf(10 + random.nextInt(990));  // $10 to $1000
            String description = getTransactionDescription(type);
            LocalDateTime transactionDate = getRandomDateBetween(lastTransactionDate, LocalDateTime.now());

            // Update balance based on transaction type
            if (type == TransactionType.DEPOSIT || type == TransactionType.TRANSFER_IN || type == TransactionType.INTEREST) {
                currentBalance = currentBalance.add(amount);
            } else {
                // For withdrawals, ensure we don't go negative
                if (currentBalance.compareTo(amount) < 0) {
                    amount = currentBalance.multiply(BigDecimal.valueOf(0.8));  // Take 80% of available balance
                }
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    continue; // Skip this transaction if amount is zero or negative
                }
                currentBalance = currentBalance.subtract(amount);
            }

            // Create transaction
            Transaction transaction = Transaction.builder()
                    .account(account)
                    .amount(amount)
                    .transactionType(type)
                    .description(description)
                    .transactionDate(transactionDate)
                    .referenceId(UUID.randomUUID().toString())
                    .balanceAfterTransaction(currentBalance)
                    .build();

            account.addTransaction(transaction);
            lastTransactionDate = transactionDate;
        }

        // Update account with final balance
        account.setBalance(currentBalance);
        accountRepository.save(account);
    }

    private AccountType getRandomAccountType() {
        return AccountType.values()[random.nextInt(AccountType.values().length)];
    }

    private TransactionType getRandomTransactionType() {
        List<TransactionType> commonTypes = Arrays.asList(
                TransactionType.DEPOSIT, TransactionType.WITHDRAWAL,
                TransactionType.TRANSFER_IN, TransactionType.TRANSFER_OUT
        );
        return commonTypes.get(random.nextInt(commonTypes.size()));
    }

    private String getTransactionDescription(TransactionType type) {
        switch (type) {
            case DEPOSIT:
                return getRandomItem(new String[]{"Salary deposit", "Check deposit", "Cash deposit", "Direct deposit"});
            case WITHDRAWAL:
                return getRandomItem(new String[]{"ATM withdrawal", "Cash withdrawal", "Debit card purchase", "Bill payment"});
            case TRANSFER_IN:
                return "Transfer from account " + getRandomAccountNumber();
            case TRANSFER_OUT:
                return "Transfer to account " + getRandomAccountNumber();
            case FEE:
                return "Monthly maintenance fee";
            case INTEREST:
                return "Interest payment";
            case PAYMENT:
                return getRandomItem(new String[]{"Utility payment", "Rent payment", "Credit card payment", "Subscription payment"});
            default:
                return "Transaction";
        }
    }

    private String getRandomAccountNumber() {
        return String.format("%010d", 1000000000 + random.nextInt(100000));
    }

    private String getRandomCustomerName() {
        String[] firstNames = {"John", "Jane", "Michael", "Emily", "David", "Sarah", "Robert", "Linda", "William", "Patricia"};
        String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Miller", "Davis", "Garcia", "Wilson", "Taylor"};
        return firstNames[random.nextInt(firstNames.length)] + " " + lastNames[random.nextInt(lastNames.length)];
    }

    private <T> T getRandomItem(T[] items) {
        return items[random.nextInt(items.length)];
    }

    private LocalDateTime getRandomDateInPast(int days) {
        return LocalDateTime.now().minusDays(random.nextInt(days));
    }

    private LocalDateTime getRandomDateBetween(LocalDateTime start, LocalDateTime end) {
        long startEpochSecond = start.toEpochSecond(java.time.ZoneOffset.UTC);
        long endEpochSecond = end.toEpochSecond(java.time.ZoneOffset.UTC);
        long randomEpochSecond = ThreadLocalRandom.current().nextLong(startEpochSecond, endEpochSecond);
        return LocalDateTime.ofEpochSecond(randomEpochSecond, 0, java.time.ZoneOffset.UTC);
    }
} 