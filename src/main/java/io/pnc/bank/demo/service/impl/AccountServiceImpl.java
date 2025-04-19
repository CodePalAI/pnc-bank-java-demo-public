package io.pnc.bank.demo.service.impl;

import io.pnc.bank.demo.dto.AccountDTO;
import io.pnc.bank.demo.dto.CreateAccountRequest;
import io.pnc.bank.demo.dto.UpdateAccountRequest;
import io.pnc.bank.demo.exception.ResourceAlreadyExistsException;
import io.pnc.bank.demo.exception.ResourceNotFoundException;
import io.pnc.bank.demo.mapper.AccountMapper;
import io.pnc.bank.demo.model.Account;
import io.pnc.bank.demo.model.AccountStatus;
import io.pnc.bank.demo.model.Transaction;
import io.pnc.bank.demo.model.TransactionType;
import io.pnc.bank.demo.repository.AccountRepository;
import io.pnc.bank.demo.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AccountDTO> getAllAccounts() {
        List<Account> accounts = accountRepository.findAll();
        return accountMapper.toDtoList(accounts);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDTO getAccountById(Long id) {
        Account account = findAccountById(id);
        return accountMapper.toDto(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDTO getAccountByAccountNumber(String accountNumber) {
        Account account = findAccountByAccountNumber(accountNumber);
        return accountMapper.toDto(account);
    }

    @Override
    @Transactional
    public AccountDTO createAccount(CreateAccountRequest createAccountRequest) {
        if (isAccountNumberExists(createAccountRequest.getAccountNumber())) {
            throw new ResourceAlreadyExistsException("Account with number " + createAccountRequest.getAccountNumber() + " already exists");
        }

        Account account = accountMapper.toEntity(createAccountRequest);
        account = accountRepository.save(account);
        
        // If there's an initial deposit, create a transaction for it
        if (createAccountRequest.getInitialDeposit() != null && createAccountRequest.getInitialDeposit().compareTo(BigDecimal.ZERO) > 0) {
            Transaction initialDeposit = Transaction.builder()
                    .account(account)
                    .amount(createAccountRequest.getInitialDeposit())
                    .transactionType(TransactionType.DEPOSIT)
                    .description("Initial deposit")
                    .transactionDate(LocalDateTime.now())
                    .referenceId(UUID.randomUUID().toString())
                    .balanceAfterTransaction(account.getBalance())
                    .build();
            
            account.addTransaction(initialDeposit);
            account = accountRepository.save(account);
        }

        return accountMapper.toDto(account);
    }

    @Override
    @Transactional
    public AccountDTO updateAccount(Long id, UpdateAccountRequest updateAccountRequest) {
        Account account = findAccountById(id);
        accountMapper.updateAccountFromDto(updateAccountRequest, account);
        account = accountRepository.save(account);
        return accountMapper.toDto(account);
    }

    @Override
    @Transactional
    public AccountDTO updateAccountStatus(Long id, AccountStatus status) {
        Account account = findAccountById(id);
        account.setStatus(status);
        account = accountRepository.save(account);
        return accountMapper.toDto(account);
    }

    @Override
    @Transactional
    public void deleteAccount(Long id) {
        Account account = findAccountById(id);
        
        // Soft delete by changing status to CLOSED
        account.setStatus(AccountStatus.CLOSED);
        accountRepository.save(account);
        
        log.info("Account with ID {} has been marked as CLOSED", id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAccountNumberExists(String accountNumber) {
        return accountRepository.existsByAccountNumber(accountNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountDTO> getAccountsByStatus(AccountStatus status) {
        List<Account> accounts = accountRepository.findByStatus(status);
        return accountMapper.toDtoList(accounts);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountDTO> getAccountsByAccountHolder(String accountHolder) {
        List<Account> accounts = accountRepository.findByAccountHolder(accountHolder);
        return accountMapper.toDtoList(accounts);
    }

    private Account findAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + id));
    }

    private Account findAccountByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with account number: " + accountNumber));
    }
} 