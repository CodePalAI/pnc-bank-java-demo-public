package io.pnc.bank.demo.mapper;

import io.pnc.bank.demo.dto.BeneficiaryDTO;
import io.pnc.bank.demo.dto.CreateBeneficiaryRequest;
import io.pnc.bank.demo.model.Account;
import io.pnc.bank.demo.model.Beneficiary;
import org.springframework.stereotype.Component;

@Component
public class BeneficiaryMapper {

        public BeneficiaryDTO toDTO(Beneficiary beneficiary) {
        if (beneficiary == null) {
            return null;
        }
        
        return BeneficiaryDTO.builder()
                .id(beneficiary.getId())
                .name(beneficiary.getName())
                .accountNumber(beneficiary.getAccountNumber())
                .emailAddress(beneficiary.getEmail())
                .description(beneficiary.getDescription())
                                .accountId(beneficiary.getAccount().getId())
                .createdAt(beneficiary.getCreatedAt())
                                .isFavorite(true)
                .build();
    }
    
        public Beneficiary toEntity(CreateBeneficiaryRequest request, Account account) {
        if (request == null) {
            return null;
        }
        
        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setName(request.getName());
        beneficiary.setAccountNumber(request.getAccountNumber());
        beneficiary.setEmail(request.getEmail());
        beneficiary.setDescription(request.getDescription());
        beneficiary.setAccount(account);
        
        return beneficiary;
    }
} 