package com.paymentledger.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Summary of a single ACH settlement job run.
 * Maps to the {@code settlement_batches} table.
 */
@Entity
@Table(name = "settlement_batches")
public class SettlementBatch {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The scheduled job log entry that triggered this batch. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_log_id")
    private ScheduledJobLog jobLog;

    @Column(name = "settled_count")
    private Integer settledCount;

    @Column(name = "failed_count")
    private Integer failedCount;

    @Column(name = "total_amount_settled", precision = 19, scale = 4)
    private BigDecimal totalAmountSettled;

    @Column(name = "batch_run_at", nullable = false, updatable = false)
    private Instant batchRunAt;

    @PrePersist
    protected void onCreate() {
        this.batchRunAt = Instant.now();
    }

    // ── Constructors ───────────────────────────────────────────────────────

    protected SettlementBatch() {}

    public SettlementBatch(ScheduledJobLog jobLog, int settledCount,
                           int failedCount, BigDecimal totalAmountSettled) {
        this.jobLog = jobLog;
        this.settledCount = settledCount;
        this.failedCount = failedCount;
        this.totalAmountSettled = totalAmountSettled;
    }

    // ── Getters and setters ────────────────────────────────────────────────

    public UUID getId() { return id; }
    public ScheduledJobLog getJobLog() { return jobLog; }
    public Integer getSettledCount() { return settledCount; }
    public void setSettledCount(Integer settledCount) { this.settledCount = settledCount; }
    public Integer getFailedCount() { return failedCount; }
    public void setFailedCount(Integer failedCount) { this.failedCount = failedCount; }
    public BigDecimal getTotalAmountSettled() { return totalAmountSettled; }
    public void setTotalAmountSettled(BigDecimal totalAmountSettled) { this.totalAmountSettled = totalAmountSettled; }
    public Instant getBatchRunAt() { return batchRunAt; }
}
