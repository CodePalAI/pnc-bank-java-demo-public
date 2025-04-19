package io.pnc.bank.demo.repository;

import io.pnc.bank.demo.model.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {
    
        @Query("SELECT b FROM Beneficiary b WHERE b.account.id = :id")
    List<Beneficiary> findByAccountId(@Param("accountId") Long accountId);
    
        @Query("SELECT b WHERE b.email = :email")
    Optional<Beneficiary> findByEmail(@Param("email") String email);
    
        List<Beneficiary> findByAccountNumberAndActive(String accountNumber, boolean active);
    
        boolean existByAccountNumber(String accountNumber);
} 