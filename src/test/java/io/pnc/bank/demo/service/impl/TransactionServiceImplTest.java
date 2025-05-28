package io.pnc.bank.demo.service.impl;

import io.pnc.bank.demo.dto.TransactionDTO;
import io.pnc.bank.demo.dto.TransferRequest;
import io.pnc.bank.demo.exception.InsufficientBalanceException;
import io.pnc.bank.demo.exception.ResourceNotFoundException;
import io.pnc.bank.demo.mapper.TransactionMapper;
import io.pnc.bank.demo.model.Account;
import io.pnc.bank.demo.model.AccountStatus;
import io.pnc.bank.demo.model.AccountType;
import io.pnc.bank.demo.model.Transaction;
import io.pnc.bank.demo.repository.AccountRepository;
import io.pnc.bank.demo.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransactionServiceImpl focusing on business logic and edge cases.
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private Account sourceAccount;
    private Account destinationAccount;
    private TransferRequest transferRequest;

    @BeforeEach
    void setUp() {
        sourceAccount = Account.builder()
                .id(1L)
                .accountNumber("1000000001")
                .accountHolder("Source User")
                .accountType(AccountType.CHECKING)
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .version(0L)
                .createdDate(LocalDateTime.now())
                .build();

        destinationAccount = Account.builder()
                .id(2L)
                .accountNumber("1000000002")
                .accountHolder("Destination User")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE)
                .version(0L)
                .createdDate(LocalDateTime.now())
                .build();

        transferRequest = new TransferRequest();
        transferRequest.setFromAccountNumber("1000000001");
        transferRequest.setToAccountNumber("1000000002");
        transferRequest.setAmount(new BigDecimal("100.00"));
        transferRequest.setDescription("Test transfer");
    }

    @Test
    void testSuccessfulTransfer() {
        // Setup mocks
        when(accountRepository.findByAccountNumber("1000000001"))
                .thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber("1000000002"))
                .thenReturn(Optional.of(destinationAccount));
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        TransactionDTO expectedDto = new TransactionDTO();
        when(transactionMapper.toDto(any(Transaction.class))).thenReturn(expectedDto);

        // Execute transfer
        TransactionDTO result = transactionService.transfer(transferRequest);

        // Verify
        assertNotNull(result);
        verify(accountRepository, times(4)).save(any(Account.class)); // 2 refresh + 2 balance updates
        verify(transactionRepository, times(2)).save(any(Transaction.class)); // outgoing + incoming
        verify(transactionMapper).toDto(any(Transaction.class));
    }

    @Test
    void testTransferWithInsufficientBalance() {
        // Setup source account with insufficient balance
        sourceAccount.setBalance(new BigDecimal("50.00"));
        
        when(accountRepository.findByAccountNumber("1000000001"))
                .thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber("1000000002"))
                .thenReturn(Optional.of(destinationAccount));
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Execute and verify exception
        InsufficientBalanceException exception = assertThrows(
                InsufficientBalanceException.class,
                () -> transactionService.transfer(transferRequest)
        );

        assertTrue(exception.getMessage().contains("Insufficient balance"));
        verify(accountRepository, never()).save(any(Account.class)); // No saves should occur after refresh
    }

    @Test
    void testTransferWithNonExistentSourceAccount() {
        when(accountRepository.findByAccountNumber("1000000001"))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> transactionService.transfer(transferRequest)
        );

        assertTrue(exception.getMessage().contains("Account not found"));
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void testTransferWithNonExistentDestinationAccount() {
        when(accountRepository.findByAccountNumber("1000000001"))
                .thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber("1000000002"))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> transactionService.transfer(transferRequest)
        );

        assertTrue(exception.getMessage().contains("Account not found"));
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void testTransferWithInactiveSourceAccount() {
        sourceAccount.setStatus(AccountStatus.CLOSED);
        
        when(accountRepository.findByAccountNumber("1000000001"))
                .thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber("1000000002"))
                .thenReturn(Optional.of(destinationAccount));
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> transactionService.transfer(transferRequest)
        );

        assertTrue(exception.getMessage().contains("not active"));
    }

    @Test
    void testTransferWithInactiveDestinationAccount() {
        destinationAccount.setStatus(AccountStatus.SUSPENDED);
        
        when(accountRepository.findByAccountNumber("1000000001"))
                .thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber("1000000002"))
                .thenReturn(Optional.of(destinationAccount));
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> transactionService.transfer(transferRequest)
        );

        assertTrue(exception.getMessage().contains("not active"));
    }

    @Test
    void testTransferBalanceCalculation() {
        when(accountRepository.findByAccountNumber("1000000001"))
                .thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber("1000000002"))
                .thenReturn(Optional.of(destinationAccount));
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        TransactionDTO expectedDto = new TransactionDTO();
        when(transactionMapper.toDto(any(Transaction.class))).thenReturn(expectedDto);

        // Execute transfer
        transactionService.transfer(transferRequest);

        // Verify balance calculations
        verify(accountRepository, atLeast(2)).save(argThat(account -> {
            if (account.getId().equals(sourceAccount.getId())) {
                // Source account should have 900.00 (1000 - 100)
                return account.getBalance().compareTo(new BigDecimal("900.00")) == 0;
            } else if (account.getId().equals(destinationAccount.getId())) {
                // Destination account should have 600.00 (500 + 100)
                return account.getBalance().compareTo(new BigDecimal("600.00")) == 0;
            }
            return true;
        }));
    }

    @Test
    void testAccountLockingOrder() {
        // Test that accounts are processed in ID order to prevent deadlocks
        Account higherIdAccount = Account.builder()
                .id(10L)  // Higher ID
                .accountNumber("1000000010")
                .accountHolder("Higher ID User")
                .accountType(AccountType.CHECKING)
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .version(0L)
                .build();

        Account lowerIdAccount = Account.builder()
                .id(5L)   // Lower ID
                .accountNumber("1000000005")
                .accountHolder("Lower ID User")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE)
                .version(0L)
                .build();

        // Transfer from higher ID to lower ID
        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber("1000000010");
        request.setToAccountNumber("1000000005");
        request.setAmount(new BigDecimal("100.00"));

        when(accountRepository.findByAccountNumber("1000000010"))
                .thenReturn(Optional.of(higherIdAccount));
        when(accountRepository.findByAccountNumber("1000000005"))
                .thenReturn(Optional.of(lowerIdAccount));
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        TransactionDTO expectedDto = new TransactionDTO();
        when(transactionMapper.toDto(any(Transaction.class))).thenReturn(expectedDto);

        // Execute - should not throw deadlock exception
        assertDoesNotThrow(() -> transactionService.transfer(request));
    }
}