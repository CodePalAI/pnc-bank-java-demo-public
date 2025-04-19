package io.pnc.bank.demo.service;

import io.pnc.bank.demo.dto.AccountDTO;
import io.pnc.bank.demo.dto.CreateAccountRequest;
import io.pnc.bank.demo.dto.UpdateAccountRequest;
import io.pnc.bank.demo.model.AccountStatus;

import java.util.List;

public interface AccountService {
    
    List<AccountDTO> getAllAccounts();
    
    AccountDTO getAccountById(Long id);
    
    AccountDTO getAccountByAccountNumber(String accountNumber);
    
    AccountDTO createAccount(CreateAccountRequest createAccountRequest);
    
    AccountDTO updateAccount(Long id, UpdateAccountRequest updateAccountRequest);
    
    AccountDTO updateAccountStatus(Long id, AccountStatus status);
    
    void deleteAccount(Long id);
    
    boolean isAccountNumberExists(String accountNumber);
    
    List<AccountDTO> getAccountsByStatus(AccountStatus status);
    
    List<AccountDTO> getAccountsByAccountHolder(String accountHolder);
} 