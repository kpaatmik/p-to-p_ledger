package com.paymentledger.service;

import com.paymentledger.entity.JobStatus;
import com.paymentledger.entity.ScheduledJobLog;
import com.paymentledger.repository.ScheduledJobLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Job logging service for scheduled job execution tracking (T-098).
 * Records job start, end, status, processed count and errors.
 */
@Service
@RequiredArgsConstructor
public class JobLoggingService {

    private final ScheduledJobLogRepository jobLogRepository;

    /**
     * T-098: Create a RUNNING job log entry at start of job execution.
     * Returns the log ID for later status updates.
     *
     * @param jobName the name of the job being executed
     * @return the job log ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID startJob(String jobName) {
        ScheduledJobLog log = new ScheduledJobLog(jobName);
        log.setStatus(JobStatus.RUNNING);
        ScheduledJobLog saved = jobLogRepository.save(log);
        return saved.getId();
    }

    /**
     * T-098: End a job and record final status, end time, processed count and errors.
     * Use REQUIRES_NEW to ensure logging persists independently of job outcome.
     *
     * @param logId the job log ID (returned by startJob)
     * @param status final job status (RUNNING, SUCCESS, FAILED)
     * @param recordsProcessed number of records successfully processed
     * @param errorDetails error message if any (may be null)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void endJob(UUID logId, JobStatus status, int recordsProcessed, String errorDetails) {
        ScheduledJobLog log = jobLogRepository.findById(logId)
                .orElseThrow(() -> new IllegalStateException("Job log not found: " + logId));

        log.setStatus(status);
        log.setEndedAt(Instant.now());
        log.setRecordsProcessed(recordsProcessed);
        log.setErrorDetails(errorDetails);
        jobLogRepository.save(log);
    }

    /**
     * T-098: Convenience method to mark a job as FAILED with error details.
     *
     * @param logId the job log ID
     * @param errorMessage the error message
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failJob(UUID logId, String errorMessage) {
        endJob(logId, JobStatus.FAILED, 0, errorMessage);
    }

    /**
     * T-098: Convenience method to mark a job as SUCCESS.
     *
     * @param logId the job log ID
     * @param recordsProcessed number of records processed successfully
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void successJob(UUID logId, int recordsProcessed) {
        endJob(logId, JobStatus.SUCCESS, recordsProcessed, null);
    }
}
