package com.paymentledger.scheduler;

import com.paymentledger.service.JobLoggingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * T-094: Daily limit reset job.
 * Runs at midnight IST.
 * The ₹50,000 P2P daily limit resets automatically via the rolling query window in FraudService.
 * This job logs the execution and ensures job-tracking records exist.
 */
@Component
@RequiredArgsConstructor
public class DailyLimitResetJob {

    private final JobLoggingService jobLoggingService;

    private static final String JOB_NAME = "DailyLimitReset";

    /**
     * Run at midnight IST (0 0 0 = 00:00:00).
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kolkata")
    public void resetDailyLimit() {
        UUID logId = jobLoggingService.startJob(JOB_NAME);

        try {
            // The daily fraud limit is computed via rolling query window in FraudService.
            // At midnight IST, queries will switch to a new calendar day.
            // No explicit reset is needed — the boundary is automatic.
            // This job simply logs the execution for audit purposes.

            jobLoggingService.successJob(logId, 0);

        } catch (Exception ex) {
            jobLoggingService.failJob(logId, "Daily limit reset failed: " + ex.getMessage());
        }
    }
}
