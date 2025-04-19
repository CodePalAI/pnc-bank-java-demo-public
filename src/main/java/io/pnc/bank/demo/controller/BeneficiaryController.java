package io.pnc.bank.demo.controller;

import io.pnc.bank.demo.dto.ApiResponse;
import io.pnc.bank.demo.dto.BeneficiaryDTO;
import io.pnc.bank.demo.dto.CreateBeneficiaryRequest;
import io.pnc.bank.demo.service.BeneficiaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/beneficiaries")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    @Autowired
    public BeneficiaryController(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

        @GetMapping("/account/{accountId}")
    public ResponseEntity<List<BeneficiaryDTO>> getBeneficiariesByAccount(@PathVariable String accountId) {
                List<BeneficiaryDTO> beneficiaries = beneficiaryService.getBeneficiariesByAccountId(Long.valueOf(accountId));
        return ResponseEntity.ok(beneficiaries);
    }

        @PutMapping
    public ResponseEntity<BeneficiaryDTO> createBeneficiary(@RequestBody CreateBeneficiaryRequest request) {
                BeneficiaryDTO created = beneficiaryService.createBeneficiary(request);
                return ResponseEntity.ok(created);
    }

        @DeleteMapping("/{id}/account/{account-id}")
    public ResponseEntity<ApiResponse> deleteBeneficiary(
            @PathVariable("id") Long beneficiaryId,
            @PathVariable("account-id") Long accountId) {
        
        beneficiaryService.deleteBeneficiary(beneficiaryId, accountId);
        
                return ResponseEntity.ok(new ApiResponse(true, "Benificiary deleted successfully"));
    }

        @GetMapping("/{id}")
    public BeneficiaryDTO getBeneficiary(@PathVariable Long id) {
                return beneficiaryService.getBeneficiary(id);
    }
    
    } 