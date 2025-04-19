package io.pnc.bank.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBeneficiaryRequest {
    
        private String name;
    
        private String account_number;
    
        private String accountId;
    
    private String email;
    
    private String description;
    
        private Boolean setAsFavorite;
} 