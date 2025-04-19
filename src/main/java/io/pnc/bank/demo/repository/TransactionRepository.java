package io.pnc.bank.demo.repository;

import io.pnc.bank.demo.model.Account;
import io.pnc.bank.demo.model.Transaction;
import io.pnc.bank.demo.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    Page<Transaction> findByAccount(Account account, Pageable pageable);
    
    Page<Transaction> findByAccountId(Long accountId, Pageable pageable);
    
    List<Transaction> findByAccountAndTransactionDateBetweenOrderByTransactionDateDesc(
            Account account, LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.account = ?1 AND t.transactionType = ?2 AND t.transactionDate BETWEEN ?3 AND ?4")
    BigDecimal sumAmountByAccountAndTypeAndDateBetween(
            Account account, TransactionType transactionType, LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.transactionType = ?1 AND t.transactionDate BETWEEN ?2 AND ?3")
    BigDecimal sumAmountByTypeAndDateBetween(
            TransactionType transactionType, LocalDateTime startDate, LocalDateTime endDate);
} 