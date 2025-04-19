package io.pnc.bank.demo.service.impl;

import io.pnc.bank.demo.dto.DepositRequest;
import io.pnc.bank.demo.dto.TransactionDTO;
import io.pnc.bank.demo.dto.TransferRequest;
import io.pnc.bank.demo.dto.WithdrawalRequest;
import io.pnc.bank.demo.exception.InsufficientBalanceException;
import io.pnc.bank.demo.exception.ResourceNotFoundException;
import io.pnc.bank.demo.mapper.TransactionMapper;
import io.pnc.bank.demo.model.Account;
import io.pnc.bank.demo.model.AccountStatus;
import io.pnc.bank.demo.model.Transaction;
import io.pnc.bank.demo.model.TransactionType;
import io.pnc.bank.demo.repository.AccountRepository;
import io.pnc.bank.demo.repository.TransactionRepository;
import io.pnc.bank.demo.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionMapper transactionMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionDTO> getTransactionsByAccountId(Long accountId, Pageable pageable) {
        Page<Transaction> transactions = transactionRepository.findByAccountId(accountId, pageable);
        return new PageImpl<>(
                transactionMapper.toDtoList(transactions.getContent()),
                pageable,
                transactions.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionDTO> getTransactionsByAccountNumber(String accountNumber, Pageable pageable) {
        Account account = findAccountByAccountNumber(accountNumber);
        Page<Transaction> transactions = transactionRepository.findByAccount(account, pageable);
        return new PageImpl<>(
                transactionMapper.toDtoList(transactions.getContent()),
                pageable,
                transactions.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionDTO getTransactionById(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + transactionId));
        return transactionMapper.toDto(transaction);
    }

    @Override
    @Transactional
    public TransactionDTO deposit(DepositRequest depositRequest) {
        Account account = findAccountByAccountNumber(depositRequest.getAccountNumber());
        
        // Ensure account is active
        validateAccountIsActive(account);
        
        // Update account balance
        BigDecimal newBalance = account.getBalance().add(depositRequest.getAmount());
        account.setBalance(newBalance);
        account = accountRepository.save(account);
        
        // Create transaction record
        Transaction transaction = Transaction.builder()
                .account(account)
                .amount(depositRequest.getAmount())
                .transactionType(TransactionType.DEPOSIT)
                .description(depositRequest.getDescription())
                .transactionDate(LocalDateTime.now())
                .referenceId(UUID.randomUUID().toString())
                .balanceAfterTransaction(newBalance)
                .build();
        
        transaction = transactionRepository.save(transaction);
        log.info("Deposit of {} processed for account {}", depositRequest.getAmount(), account.getAccountNumber());
        
        return transactionMapper.toDto(transaction);
    }

    @Override
    @Transactional
    public TransactionDTO withdraw(WithdrawalRequest withdrawalRequest) {
        Account account = findAccountByAccountNumber(withdrawalRequest.getAccountNumber());
        
        // Ensure account is active
        validateAccountIsActive(account);
        
        // Check sufficient balance
        if (account.getBalance().compareTo(withdrawalRequest.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance for withdrawal from account " + account.getAccountNumber());
        }
        
        // Update account balance
        BigDecimal newBalance = account.getBalance().subtract(withdrawalRequest.getAmount());
        account.setBalance(newBalance);
        account = accountRepository.save(account);
        
        // Create transaction record
        Transaction transaction = Transaction.builder()
                .account(account)
                .amount(withdrawalRequest.getAmount())
                .transactionType(TransactionType.WITHDRAWAL)
                .description(withdrawalRequest.getDescription())
                .transactionDate(LocalDateTime.now())
                .referenceId(UUID.randomUUID().toString())
                .balanceAfterTransaction(newBalance)
                .build();
        
        transaction = transactionRepository.save(transaction);
        log.info("Withdrawal of {} processed for account {}", withdrawalRequest.getAmount(), account.getAccountNumber());
        
        return transactionMapper.toDto(transaction);
    }

    @Override
    @Transactional
    public TransactionDTO transfer(TransferRequest transferRequest) {
        Account sourceAccount = findAccountByAccountNumber(transferRequest.getFromAccountNumber());
        Account destinationAccount = findAccountByAccountNumber(transferRequest.getToAccountNumber());
        
        // Ensure accounts are active
        validateAccountIsActive(sourceAccount);
        validateAccountIsActive(destinationAccount);
        
        // Check sufficient balance
        if (sourceAccount.getBalance().compareTo(transferRequest.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance for transfer from account " + sourceAccount.getAccountNumber());
        }
        
        // Generate a common reference ID for both transactions
        String referenceId = UUID.randomUUID().toString();
        LocalDateTime transactionDate = LocalDateTime.now();
        
        // Update source account balance
        BigDecimal sourceNewBalance = sourceAccount.getBalance().subtract(transferRequest.getAmount());
        sourceAccount.setBalance(sourceNewBalance);
        sourceAccount = accountRepository.save(sourceAccount);
        
        // Create outgoing transaction
        Transaction outgoingTransaction = Transaction.builder()
                .account(sourceAccount)
                .amount(transferRequest.getAmount())
                .transactionType(TransactionType.TRANSFER_OUT)
                .description("Transfer to " + destinationAccount.getAccountNumber() + 
                        (transferRequest.getDescription() != null ? ": " + transferRequest.getDescription() : ""))
                .transactionDate(transactionDate)
                .referenceId(referenceId)
                .balanceAfterTransaction(sourceNewBalance)
                .build();
        
        outgoingTransaction = transactionRepository.save(outgoingTransaction);
        
        // Update destination account balance
        BigDecimal destNewBalance = destinationAccount.getBalance().add(transferRequest.getAmount());
        destinationAccount.setBalance(destNewBalance);
        destinationAccount = accountRepository.save(destinationAccount);
        
        // Create incoming transaction
        Transaction incomingTransaction = Transaction.builder()
                .account(destinationAccount)
                .amount(transferRequest.getAmount())
                .transactionType(TransactionType.TRANSFER_IN)
                .description("Transfer from " + sourceAccount.getAccountNumber() + 
                        (transferRequest.getDescription() != null ? ": " + transferRequest.getDescription() : ""))
                .transactionDate(transactionDate)
                .referenceId(referenceId)
                .balanceAfterTransaction(destNewBalance)
                .build();
        
        transactionRepository.save(incomingTransaction);
        
        log.info("Transfer of {} from account {} to account {} processed", 
                transferRequest.getAmount(), sourceAccount.getAccountNumber(), destinationAccount.getAccountNumber());
        
        return transactionMapper.toDto(outgoingTransaction);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getDailyTransactionSummary(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        
        Map<String, BigDecimal> summary = new HashMap<>();
        
        // Get total deposits
        BigDecimal totalDeposits = transactionRepository.sumAmountByTypeAndDateBetween(
                TransactionType.DEPOSIT, startOfDay, endOfDay);
        summary.put("totalDeposits", totalDeposits != null ? totalDeposits : BigDecimal.ZERO);
        
        // Get total withdrawals
        BigDecimal totalWithdrawals = transactionRepository.sumAmountByTypeAndDateBetween(
                TransactionType.WITHDRAWAL, startOfDay, endOfDay);
        summary.put("totalWithdrawals", totalWithdrawals != null ? totalWithdrawals : BigDecimal.ZERO);
        
        // Count active accounts (could be moved to AccountService)
        long activeAccountCount = accountRepository.findByStatus(AccountStatus.ACTIVE).size();
        summary.put("activeAccountCount", BigDecimal.valueOf(activeAccountCount));
        
        return summary;
    }

    @Override
    public byte[] generateTransactionReport(LocalDate startDate, LocalDate endDate, String format) {
        // Implementation for generating CSV or PDF report would go here
        // For simplicity, we'll just return a placeholder
        return "Transaction Report from ".concat(startDate.toString())
                .concat(" to ").concat(endDate.toString())
                .getBytes();
    }
    
    private Account findAccountByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with account number: " + accountNumber));
    }
    
    private void validateAccountIsActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account " + account.getAccountNumber() + " is not active. Current status: " + account.getStatus());
        }
    }
} 