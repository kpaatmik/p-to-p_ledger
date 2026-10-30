package com.paymentledger.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Execution record for every scheduled background job run.
 * Maps to the {@code scheduled_job_log} table.
 */
@Entity
@Table(
    name = "scheduled_job_log",
    indexes = {
        @Index(name = "idx_job_log_job_name",   columnList = "job_name"),
        @Index(name = "idx_job_log_started_at", columnList = "started_at")
    }
)
public class ScheduledJobLog {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobStatus status;

    @Column(name = "records_processed")
    private Integer recordsProcessed;

    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;

    // ── Constructors ───────────────────────────────────────────────────────

    protected ScheduledJobLog() {}

    public ScheduledJobLog(String jobName) {
        this.jobName = jobName;
        this.startedAt = Instant.now();
        this.status = JobStatus.RUNNING;
    }

    // ── Getters and setters ────────────────────────────────────────────────

    public UUID getId() { return id; }
    public String getJobName() { return jobName; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }
    public Integer getRecordsProcessed() { return recordsProcessed; }
    public void setRecordsProcessed(Integer recordsProcessed) { this.recordsProcessed = recordsProcessed; }
    public String getErrorDetails() { return errorDetails; }
    public void setErrorDetails(String errorDetails) { this.errorDetails = errorDetails; }
}
