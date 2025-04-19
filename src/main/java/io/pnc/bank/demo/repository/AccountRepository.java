package io.pnc.bank.demo.repository;

import io.pnc.bank.demo.model.Account;
import io.pnc.bank.demo.model.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    Optional<Account> findByAccountNumber(String accountNumber);
    
    boolean existsByAccountNumber(String accountNumber);
    
    List<Account> findByStatus(AccountStatus status);
    
    List<Account> findByAccountHolder(String accountHolder);
} 