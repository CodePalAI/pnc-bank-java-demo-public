package io.pnc.bank.demo.controller;

import io.pnc.bank.demo.dto.AccountDTO;
import io.pnc.bank.demo.dto.CreateAccountRequest;
import io.pnc.bank.demo.dto.TransactionDTO;
import io.pnc.bank.demo.dto.UpdateAccountRequest;
import io.pnc.bank.demo.model.AccountStatus;
import io.pnc.bank.demo.model.AccountType;
import io.pnc.bank.demo.service.AccountService;
import io.pnc.bank.demo.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class WebAccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    @GetMapping
    public String getAllAccounts(Model model) {
        model.addAttribute("accounts", accountService.getAllAccounts());
        return "accounts/list";
    }

    @GetMapping("/{id}")
    public String getAccountDetails(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        
        AccountDTO account = accountService.getAccountById(id);
        model.addAttribute("account", account);
        
        Page<TransactionDTO> transactions = transactionService.getTransactionsByAccountId(
                id, PageRequest.of(page, size, Sort.by("transactionDate").descending()));
        
        model.addAttribute("transactions", transactions);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", transactions.getTotalPages());
        
        return "accounts/details";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("createAccountRequest", new CreateAccountRequest());
        model.addAttribute("accountTypes", AccountType.values());
        return "accounts/create";
    }

    @PostMapping("/create")
    public String createAccount(
            @Valid @ModelAttribute("createAccountRequest") CreateAccountRequest createAccountRequest,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        if (result.hasErrors()) {
            model.addAttribute("accountTypes", AccountType.values());
            return "accounts/create";
        }
        
        try {
            AccountDTO newAccount = accountService.createAccount(createAccountRequest);
            redirectAttributes.addFlashAttribute("success", "Account created successfully");
            return "redirect:/accounts/" + newAccount.getId();
        } catch (Exception e) {
            model.addAttribute("accountTypes", AccountType.values());
            model.addAttribute("error", e.getMessage());
            return "accounts/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        AccountDTO account = accountService.getAccountById(id);
        
        UpdateAccountRequest updateAccountRequest = UpdateAccountRequest.builder()
                .accountHolder(account.getAccountHolder())
                .accountType(account.getAccountType())
                .status(account.getStatus())
                .build();
        
        model.addAttribute("id", id);
        model.addAttribute("updateAccountRequest", updateAccountRequest);
        model.addAttribute("accountTypes", AccountType.values());
        model.addAttribute("accountStatuses", AccountStatus.values());
        
        return "accounts/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateAccount(
            @PathVariable Long id,
            @Valid @ModelAttribute("updateAccountRequest") UpdateAccountRequest updateAccountRequest,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        if (result.hasErrors()) {
            model.addAttribute("id", id);
            model.addAttribute("accountTypes", AccountType.values());
            model.addAttribute("accountStatuses", AccountStatus.values());
            return "accounts/edit";
        }
        
        try {
            accountService.updateAccount(id, updateAccountRequest);
            redirectAttributes.addFlashAttribute("success", "Account updated successfully");
            return "redirect:/accounts/" + id;
        } catch (Exception e) {
            model.addAttribute("id", id);
            model.addAttribute("accountTypes", AccountType.values());
            model.addAttribute("accountStatuses", AccountStatus.values());
            model.addAttribute("error", e.getMessage());
            return "accounts/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteAccount(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        
        try {
            accountService.deleteAccount(id);
            redirectAttributes.addFlashAttribute("success", "Account marked as closed successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/accounts";
    }
} 