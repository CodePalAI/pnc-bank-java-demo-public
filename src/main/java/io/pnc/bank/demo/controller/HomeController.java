package io.pnc.bank.demo.controller;

import io.pnc.bank.demo.service.AccountService;
import io.pnc.bank.demo.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    @GetMapping("/")
    public String home(Model model) {
        // Get a summary for the dashboard
        Map<String, Object> summary = Map.of(
                "totalAccounts", accountService.getAllAccounts().size(),
                "dailyTransactions", transactionService.getDailyTransactionSummary(LocalDate.now())
        );
        
        model.addAttribute("summary", summary);
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "redirect:/";
    }
} 