package com.paymentledger.scheduler;

import com.paymentledger.service.AccountLockService;
import com.paymentledger.service.JobLoggingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * T-095: Account auto-unlock job.
 * Runs every 5 minutes.
 * Calls the existing AccountLockService.unlockEligibleAccounts() to unlock expired locks.
 */
@Component
@RequiredArgsConstructor
public class AccountUnlockJob {

    private final AccountLockService accountLockService;
    private final JobLoggingService jobLoggingService;

    private static final String JOB_NAME = "AccountAutoUnlock";

    /**
     * Run every 5 minutes (300,000 milliseconds).
     * Fixed delay means 5 minutes between executions (not between completions).
     */
    @Scheduled(fixedDelay = 300000)
    public void unlockEligibleAccounts() {
        UUID logId = jobLoggingService.startJob(JOB_NAME);

        try {
            int unlockedCount = accountLockService.unlockEligibleAccounts();
            jobLoggingService.successJob(logId, unlockedCount);

        } catch (Exception ex) {
            jobLoggingService.failJob(logId, "Account unlock job failed: " + ex.getMessage());
        }
    }
}
