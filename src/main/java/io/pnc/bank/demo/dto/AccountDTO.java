package io.pnc.bank.demo.dto;

import io.pnc.bank.demo.model.AccountStatus;
import io.pnc.bank.demo.model.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class AccountDTO {
    private Long id;
    
    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Account number must be exactly 10 digits")
    private String accountNumber;
    
    @NotBlank(message = "Account holder name is required")
    private String accountHolder;
    
    @NotNull(message = "Account type is required")
    private AccountType accountType;
    
    private BigDecimal balance;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
    private AccountStatus status;
} 