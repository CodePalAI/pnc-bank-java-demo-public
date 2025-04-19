package io.pnc.bank.demo.controller;

import io.pnc.bank.demo.dto.AccountDTO;
import io.pnc.bank.demo.dto.ApiResponse;
import io.pnc.bank.demo.dto.CreateAccountRequest;
import io.pnc.bank.demo.dto.UpdateAccountRequest;
import io.pnc.bank.demo.model.AccountStatus;
import io.pnc.bank.demo.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountDTO>>> getAllAccounts() {
        List<AccountDTO> accounts = accountService.getAllAccounts();
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountDTO>> getAccountById(@PathVariable Long id) {
        AccountDTO account = accountService.getAccountById(id);
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountDTO>> getAccountByAccountNumber(@PathVariable String accountNumber) {
        AccountDTO account = accountService.getAccountByAccountNumber(accountNumber);
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AccountDTO>> createAccount(@Valid @RequestBody CreateAccountRequest createAccountRequest) {
        AccountDTO newAccount = accountService.createAccount(createAccountRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created successfully", newAccount));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountDTO>> updateAccount(
            @PathVariable Long id, 
            @Valid @RequestBody UpdateAccountRequest updateAccountRequest) {
        AccountDTO updatedAccount = accountService.updateAccount(id, updateAccountRequest);
        return ResponseEntity.ok(ApiResponse.success("Account updated successfully", updatedAccount));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AccountDTO>> updateAccountStatus(
            @PathVariable Long id, 
            @RequestParam AccountStatus status) {
        AccountDTO updatedAccount = accountService.updateAccountStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Account status updated successfully", updatedAccount));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.ok(ApiResponse.success("Account deleted successfully", null));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<AccountDTO>>> getAccountsByStatus(@PathVariable AccountStatus status) {
        List<AccountDTO> accounts = accountService.getAccountsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @GetMapping("/holder/{accountHolder}")
    public ResponseEntity<ApiResponse<List<AccountDTO>>> getAccountsByAccountHolder(
            @PathVariable String accountHolder) {
        List<AccountDTO> accounts = accountService.getAccountsByAccountHolder(accountHolder);
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }
} 