package io.pnc.bank.demo.service.impl;

import io.pnc.bank.demo.model.AccountStatus;
import io.pnc.bank.demo.model.TransactionType;
import io.pnc.bank.demo.repository.AccountRepository;
import io.pnc.bank.demo.repository.TransactionRepository;
import io.pnc.bank.demo.service.ReportingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportingServiceImpl implements ReportingService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    // Run daily at midnight
    @Scheduled(cron = "0 0 0 * * ?") 
    @Override
    public void generateDailyReport() {
        log.info("Generating daily transaction report");
        
        LocalDate yesterday = LocalDate.now().minusDays(1);
        byte[] reportData = generateTransactionReport(yesterday, yesterday, "csv");
        
        // In a real application, this could:
        // - Save to a database
        // - Email to stakeholders
        // - Upload to a file system
        // - Push to a document management system
        
        log.info("Daily report generated successfully, size: {} bytes", reportData.length);
    }

    @Override
    public byte[] generateTransactionReport(LocalDate startDate, LocalDate endDate, String format) {
        log.info("Generating transaction report from {} to {} in {} format", startDate, endDate, format);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        // Get account statistics
        long totalAccounts = accountRepository.count();
        long activeAccounts = accountRepository.findByStatus(AccountStatus.ACTIVE).size();
        
        // Get transaction statistics
        BigDecimal totalDeposits = transactionRepository.sumAmountByTypeAndDateBetween(
                TransactionType.DEPOSIT, startDateTime, endDateTime);
        if (totalDeposits == null) totalDeposits = BigDecimal.ZERO;
        
        BigDecimal totalWithdrawals = transactionRepository.sumAmountByTypeAndDateBetween(
                TransactionType.WITHDRAWAL, startDateTime, endDateTime);
        if (totalWithdrawals == null) totalWithdrawals = BigDecimal.ZERO;
        
        BigDecimal totalTransfersIn = transactionRepository.sumAmountByTypeAndDateBetween(
                TransactionType.TRANSFER_IN, startDateTime, endDateTime);
        if (totalTransfersIn == null) totalTransfersIn = BigDecimal.ZERO;
        
        BigDecimal totalTransfersOut = transactionRepository.sumAmountByTypeAndDateBetween(
                TransactionType.TRANSFER_OUT, startDateTime, endDateTime);
        if (totalTransfersOut == null) totalTransfersOut = BigDecimal.ZERO;
        
        // Format as CSV
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        StringBuilder reportBuilder = new StringBuilder();
        
        reportBuilder.append("PNC Bank Transaction Report\n");
        reportBuilder.append("Report Period: ").append(startDate).append(" to ").append(endDate).append("\n");
        reportBuilder.append("Generated on: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");
        
        reportBuilder.append("Account Statistics\n");
        reportBuilder.append("Total Accounts,").append(totalAccounts).append("\n");
        reportBuilder.append("Active Accounts,").append(activeAccounts).append("\n\n");
        
        reportBuilder.append("Transaction Statistics\n");
        reportBuilder.append("Total Deposits,").append(totalDeposits).append("\n");
        reportBuilder.append("Total Withdrawals,").append(totalWithdrawals).append("\n");
        reportBuilder.append("Total Transfers In,").append(totalTransfersIn).append("\n");
        reportBuilder.append("Total Transfers Out,").append(totalTransfersOut).append("\n");
        reportBuilder.append("Net Movement,").append(totalDeposits.add(totalTransfersIn).subtract(totalWithdrawals).subtract(totalTransfersOut)).append("\n");
        
        // In a real implementation, we would implement PDF generation for the pdf format option
        // For now, just return the same content for all formats
        
        return reportBuilder.toString().getBytes(StandardCharsets.UTF_8);
    }
} 