package io.pnc.bank.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiaryDTO {
    
    private Long id;
    
        private String name;
    
    private String accountNumber;
    
        private String emailAddress;
    
    private String description;
    
        private Long accountId;
    
    private LocalDateTime createdAt;
    
        private Boolean isFavorite;
} 