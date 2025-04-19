package io.pnc.bank.demo.controller;

import io.pnc.bank.demo.dto.*;
import io.pnc.bank.demo.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/account/{accountId}")
    public ResponseEntity<ApiResponse<Page<TransactionDTO>>> getTransactionsByAccountId(
            @PathVariable Long accountId,
            Pageable pageable) {
        Page<TransactionDTO> transactions = transactionService.getTransactionsByAccountId(accountId, pageable);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/account/number/{accountNumber}")
    public ResponseEntity<ApiResponse<Page<TransactionDTO>>> getTransactionsByAccountNumber(
            @PathVariable String accountNumber,
            Pageable pageable) {
        Page<TransactionDTO> transactions = transactionService.getTransactionsByAccountNumber(accountNumber, pageable);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionDTO>> getTransactionById(@PathVariable Long transactionId) {
        TransactionDTO transaction = transactionService.getTransactionById(transactionId);
        return ResponseEntity.ok(ApiResponse.success(transaction));
    }

    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<TransactionDTO>> deposit(@Valid @RequestBody DepositRequest depositRequest) {
        TransactionDTO transaction = transactionService.deposit(depositRequest);
        return ResponseEntity.ok(ApiResponse.success("Deposit processed successfully", transaction));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<TransactionDTO>> withdraw(@Valid @RequestBody WithdrawalRequest withdrawalRequest) {
        TransactionDTO transaction = transactionService.withdraw(withdrawalRequest);
        return ResponseEntity.ok(ApiResponse.success("Withdrawal processed successfully", transaction));
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransactionDTO>> transfer(@Valid @RequestBody TransferRequest transferRequest) {
        TransactionDTO transaction = transactionService.transfer(transferRequest);
        return ResponseEntity.ok(ApiResponse.success("Transfer processed successfully", transaction));
    }

    @GetMapping("/summary/daily")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> getDailyTransactionSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate reportDate = date != null ? date : LocalDate.now();
        Map<String, BigDecimal> summary = transactionService.getDailyTransactionSummary(reportDate);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/report")
    public ResponseEntity<byte[]> generateTransactionReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "csv") String format) {
        
        byte[] reportData = transactionService.generateTransactionReport(startDate, endDate, format);
        
        String filename = "transaction_report_" + startDate + "_to_" + endDate + "." + format.toLowerCase();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", filename);
        
        if ("csv".equalsIgnoreCase(format)) {
            headers.setContentType(MediaType.parseMediaType("text/csv"));
        } else if ("pdf".equalsIgnoreCase(format)) {
            headers.setContentType(MediaType.APPLICATION_PDF);
        } else {
            headers.setContentType(MediaType.TEXT_PLAIN);
        }
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(reportData);
    }
} 