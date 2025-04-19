package io.pnc.bank.demo.dto;

import io.pnc.bank.demo.model.AccountStatus;
import io.pnc.bank.demo.model.AccountType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAccountRequest {
    
    @NotBlank(message = "Account holder name is required")
    private String accountHolder;
    
    private AccountType accountType;
    
    private AccountStatus status;
} 