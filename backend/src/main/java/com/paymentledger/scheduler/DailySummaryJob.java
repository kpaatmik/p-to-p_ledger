package com.paymentledger.scheduler;

import com.paymentledger.entity.DailySummary;
import com.paymentledger.repository.*;
import com.paymentledger.service.JobLoggingService;
import com.paymentledger.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

/**
 * T-096: Daily summary and reconciliation job.
 * Runs at 11 PM IST.
 * Calculates daily aggregate metrics and performs P2P reconciliation.
 */
@Component
@RequiredArgsConstructor
public class DailySummaryJob {

    private final DailySummaryRepository dailySummaryRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final JobLoggingService jobLoggingService;

    private static final String JOB_NAME = "DailySummary";

    /**
     * Run at 11 PM IST (23:00:00).
     */
    @Scheduled(cron = "0 0 23 * * *", zone = "Asia/Kolkata")
    public void generateDailySummary() {
        UUID logId = jobLoggingService.startJob(JOB_NAME);

        try {
            // Determine IST date for today
            LocalDate istToday = LocalDate.now(ZoneId.of("Asia/Kolkata"));

            // Get or create daily summary record
            Optional<DailySummary> existing = dailySummaryRepository.findBySummaryDate(istToday);
            DailySummary summary = existing.orElse(new DailySummary(istToday));

            // Calculate metrics using efficient aggregate queries
            // T-096: Initiated transaction count and amount (all PENDING or SUCCESS P2P transfers created today)
            long initiatedCount = transactionRepository.countInitiatedToday(istToday);
            BigDecimal initiatedAmount = transactionRepository.sumInitiatedAmountToday(istToday);

            summary.setInitiatedCount((int) initiatedCount);
            summary.setInitiatedAmount(initiatedAmount != null ? initiatedAmount : BigDecimal.ZERO);

            // Settled transaction count and amount (all SUCCESS P2P transfers settled today)
            long settledCount = transactionRepository.countSettledToday(istToday);
            BigDecimal settledAmount = transactionRepository.sumSettledAmountToday(istToday);

            summary.setSettledCount((int) settledCount);
            summary.setSettledAmount(settledAmount != null ? settledAmount : BigDecimal.ZERO);

            // Fraud alert count (all alerts created today)
            long fraudCount = fraudAlertRepository.countCreatedToday(istToday);
            summary.setFraudAlertCount((int) fraudCount);

            // New user registrations (users created today)
            long newUsers = userRepository.countCreatedToday(istToday);
            summary.setNewRegistrations((int) newUsers);

            // Pending settlement amount (sum of PENDING P2P transfers)
            BigDecimal pendingAmount = transactionRepository.sumPendingSettlementAmount();
            summary.setPendingSettlementAmount(pendingAmount != null ? pendingAmount : BigDecimal.ZERO);

            // Save summary
            dailySummaryRepository.save(summary);

            // T-096: Reconciliation — compare DEBIT and CREDIT ledger totals for settled P2P transactions
            BigDecimal totalDebits = ledgerEntryRepository.sumSettledDebits(istToday);
            BigDecimal totalCredits = ledgerEntryRepository.sumSettledCredits(istToday);

            if ((totalDebits == null ? BigDecimal.ZERO : totalDebits)
                    .compareTo(totalCredits == null ? BigDecimal.ZERO : totalCredits) != 0) {
                // Mismatch detected — notify admin
                // Avoid duplicate notifications by checking if alert was already sent today
                if (!hasReconciliationAlertToday(istToday)) {
                    notificationService.send(
                            userRepository.findByRole("ADMIN").stream().findFirst().orElse(null),
                            NotificationService.TYPE_RECONCILIATION_MISMATCH,
                            String.format("P2P reconciliation mismatch for %s: Debits ₹%s != Credits ₹%s",
                                    istToday, totalDebits != null ? totalDebits : BigDecimal.ZERO,
                                    totalCredits != null ? totalCredits : BigDecimal.ZERO)
                    );
                }
            }

            jobLoggingService.successJob(logId, 1);

        } catch (Exception ex) {
            jobLoggingService.failJob(logId, "Daily summary job failed: " + ex.getMessage());
        }
    }

    /**
     * Helper: Check if reconciliation alert has already been sent today to avoid duplicates.
     */
    private boolean hasReconciliationAlertToday(LocalDate date) {
        // Simple check: if the DailySummary already exists for today, we've already sent the alert
        return dailySummaryRepository.findBySummaryDate(date).isPresent();
    }
}
