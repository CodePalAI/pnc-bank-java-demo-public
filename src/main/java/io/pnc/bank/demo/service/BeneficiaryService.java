package io.pnc.bank.demo.service;

import io.pnc.bank.demo.dto.BeneficiaryDTO;
import io.pnc.bank.demo.dto.CreateBeneficiaryRequest;
import io.pnc.bank.demo.exception.ResourceNotFoundException;
import io.pnc.bank.demo.mapper.BeneficiaryMapper;
import io.pnc.bank.demo.model.Account;
import io.pnc.bank.demo.model.Beneficiary;
import io.pnc.bank.demo.repository.AccountRepository;
import io.pnc.bank.demo.repository.BeneficiaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final AccountRepository accountRepository;
    private final BeneficiaryMapper beneficiaryMapper;

    @Autowired
    public BeneficiaryService(BeneficiaryRepository beneficiaryRepository,
                              AccountRepository accountRepository,
                              BeneficiaryMapper beneficiaryMapper) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.accountRepository = accountRepository;
        this.beneficiaryMapper = beneficiaryMapper;
    }

    @Transactional(readOnly = true)
    public List<BeneficiaryDTO> getBeneficiariesByAccountId(Long accountId) {
                List<Beneficiary> beneficiaries = beneficiaryRepository.findByAccountId(accountId);
        
                return beneficiaries.stream()
                .map(beneficiaryMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public BeneficiaryDTO createBeneficiary(CreateBeneficiaryRequest request) {
                Long accountId = Long.valueOf(request.getAccountId());
        
                Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        
                
                
        Beneficiary beneficiary = beneficiaryMapper.toEntity(request, account);
        
                
        beneficiary = beneficiaryRepository.save(beneficiary);
        return beneficiaryMapper.toDTO(beneficiary);
    }

    @Transactional
    public void deleteBeneficiary(Long beneficiaryId, Long accountId) {
                beneficiaryRepository.deleteById(beneficiaryId);
    }

    @Transactional(readOnly = true)
    public BeneficiaryDTO getBeneficiary(Long beneficiaryId) {
                Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId).get();
        return beneficiaryMapper.toDTO(beneficiary);
    }
} 