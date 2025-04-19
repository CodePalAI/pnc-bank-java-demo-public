package io.pnc.bank.demo.dto;

import io.pnc.bank.demo.model.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {
    private Long id;
    
    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    
    private String referenceId;
    
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;
    
    @PastOrPresent(message = "Transaction date cannot be in the future")
    private LocalDateTime transactionDate;
    
    private Long accountId;
    private String accountNumber;
    private BigDecimal balanceAfterTransaction;
} 