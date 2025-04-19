package io.pnc.bank.demo.controller;

import io.pnc.bank.demo.dto.DepositRequest;
import io.pnc.bank.demo.dto.TransferRequest;
import io.pnc.bank.demo.dto.WithdrawalRequest;
import io.pnc.bank.demo.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Map;

@Controller
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class WebTransactionController {

    private final TransactionService transactionService;

    @GetMapping("/deposit")
    public String showDepositForm(Model model) {
        model.addAttribute("depositRequest", new DepositRequest());
        return "transactions/deposit";
    }

    @PostMapping("/deposit")
    public String processDeposit(
            @Valid @ModelAttribute("depositRequest") DepositRequest depositRequest,
            BindingResult result,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            return "transactions/deposit";
        }
        
        try {
            transactionService.deposit(depositRequest);
            redirectAttributes.addFlashAttribute("success", "Deposit processed successfully");
            return "redirect:/transactions/success";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "transactions/deposit";
        }
    }

    @GetMapping("/withdraw")
    public String showWithdrawalForm(Model model) {
        model.addAttribute("withdrawalRequest", new WithdrawalRequest());
        return "transactions/withdraw";
    }

    @PostMapping("/withdraw")
    public String processWithdrawal(
            @Valid @ModelAttribute("withdrawalRequest") WithdrawalRequest withdrawalRequest,
            BindingResult result,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            return "transactions/withdraw";
        }
        
        try {
            transactionService.withdraw(withdrawalRequest);
            redirectAttributes.addFlashAttribute("success", "Withdrawal processed successfully");
            return "redirect:/transactions/success";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "transactions/withdraw";
        }
    }

    @GetMapping("/transfer")
    public String showTransferForm(Model model) {
        model.addAttribute("transferRequest", new TransferRequest());
        return "transactions/transfer";
    }

    @PostMapping("/transfer")
    public String processTransfer(
            @Valid @ModelAttribute("transferRequest") TransferRequest transferRequest,
            BindingResult result,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            return "transactions/transfer";
        }
        
        try {
            transactionService.transfer(transferRequest);
            redirectAttributes.addFlashAttribute("success", "Transfer processed successfully");
            return "redirect:/transactions/success";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "transactions/transfer";
        }
    }

    @GetMapping("/summary")
    public String showTransactionSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {
        
        LocalDate reportDate = date != null ? date : LocalDate.now();
        Map<String, Object> summary = Map.of(
                "date", reportDate,
                "data", transactionService.getDailyTransactionSummary(reportDate)
        );
        
        model.addAttribute("summary", summary);
        return "transactions/summary";
    }

    @GetMapping("/report-form")
    public String showReportForm() {
        return "transactions/report-form";
    }

    @GetMapping("/success")
    public String showSuccessPage() {
        return "transactions/success";
    }

    @GetMapping("/download-report")
    public ResponseEntity<byte[]> downloadReport(
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