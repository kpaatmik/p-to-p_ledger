package com.paymentledger.scheduler;

import com.paymentledger.entity.JobStatus;
import com.paymentledger.entity.TransactionStatus;
import com.paymentledger.repository.ScheduledJobLogRepository;
import com.paymentledger.repository.TransactionRepository;
import com.paymentledger.service.JobLoggingService;
import com.paymentledger.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * T-091: Settlement processor job.
 * Runs hourly using Asia/Kolkata timezone.
 * Processes PENDING P2P transactions whose settlement_due_date is due.
 * Each transaction is settled independently; failure of one does not stop others.
 */
@Component
@RequiredArgsConstructor
public class SettlementJob {

    private final TransactionRepository transactionRepository;
    private final SettlementService settlementService;
    private final JobLoggingService jobLoggingService;

    private static final String JOB_NAME = "SettlementProcessor";
    private static final int BATCH_SIZE = 100;

    /**
     * Run hourly at the top of every hour (IST).
     */
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Kolkata")
    public void processSettlements() {
        UUID logId = jobLoggingService.startJob(JOB_NAME);

        try {
            // Determine current date in Asia/Kolkata timezone
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

            int totalProcessed = 0;
            int pageNumber = 0;

            // Process in bounded batches to avoid loading unbounded collection
            while (true) {
                Pageable pageable = PageRequest.of(pageNumber, BATCH_SIZE);
                var page = transactionRepository.findByStatusAndSettlementDueDateLessThanEqual(
                        TransactionStatus.PENDING,
                        today,
                        pageable
                );

                if (page.isEmpty()) {
                    break;
                }

                // Process each transaction independently
                for (var transaction : page.getContent()) {
                    try {
                        boolean settled = settlementService.settleTransaction(transaction.getId());
                        if (settled) {
                            totalProcessed++;
                        }
                    } catch (Exception ex) {
                        // Log exception and continue with next transaction
                        System.err.println("Failed to settle transaction " + transaction.getId() + ": " + ex.getMessage());
                    }
                }

                // Stop if no more pages
                if (!page.hasNext()) {
                    break;
                }

                pageNumber++;
            }

            // Record successful completion
            jobLoggingService.successJob(logId, totalProcessed);

        } catch (Exception ex) {
            jobLoggingService.failJob(logId, "Settlement job failed: " + ex.getMessage());
        }
    }
}
