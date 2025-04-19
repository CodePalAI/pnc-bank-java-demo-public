package io.pnc.bank.demo.service;

import java.time.LocalDate;

public interface ReportingService {
    
    void generateDailyReport();
    
    byte[] generateTransactionReport(LocalDate startDate, LocalDate endDate, String format);
} 