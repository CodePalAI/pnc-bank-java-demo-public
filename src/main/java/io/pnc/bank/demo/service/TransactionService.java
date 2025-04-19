package io.pnc.bank.demo.service;

import io.pnc.bank.demo.dto.DepositRequest;
import io.pnc.bank.demo.dto.TransactionDTO;
import io.pnc.bank.demo.dto.TransferRequest;
import io.pnc.bank.demo.dto.WithdrawalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public interface TransactionService {
    
    Page<TransactionDTO> getTransactionsByAccountId(Long accountId, Pageable pageable);
    
    Page<TransactionDTO> getTransactionsByAccountNumber(String accountNumber, Pageable pageable);
    
    TransactionDTO getTransactionById(Long transactionId);
    
    TransactionDTO deposit(DepositRequest depositRequest);
    
    TransactionDTO withdraw(WithdrawalRequest withdrawalRequest);
    
    TransactionDTO transfer(TransferRequest transferRequest);
    
    Map<String, BigDecimal> getDailyTransactionSummary(LocalDate date);
    
    byte[] generateTransactionReport(LocalDate startDate, LocalDate endDate, String format);
} 