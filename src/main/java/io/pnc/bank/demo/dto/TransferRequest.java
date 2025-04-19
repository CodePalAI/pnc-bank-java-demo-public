package io.pnc.bank.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {
    
    @NotBlank(message = "Source account number is required")
    private String fromAccountNumber;
    
    @NotBlank(message = "Destination account number is required")
    private String toAccountNumber;
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;
} 