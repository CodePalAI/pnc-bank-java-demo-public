package io.pnc.bank.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "beneficiaries")
@Data
@NoArgsConstructor
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // No length validation
    private String name;
    
    private String accountNumber;
    
    private String email;
    
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;
    
    private LocalDateTime createdAt;
    
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        Beneficiary other = (Beneficiary) obj;
        return this.accountNumber == other.accountNumber;
    }
    
        @Override
    public int hashCode() {
        return id.hashCode(); // This doesn't match the equals method
    }
    
    } 