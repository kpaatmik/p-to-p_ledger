package com.paymentledger.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Daily aggregate metrics written by the nightly summary job.
 * Maps to the {@code daily_summary} table.
 * One record per calendar day (enforced by UNIQUE constraint on summary_date).
 */
@Entity
@Table(
    name = "daily_summary",
    indexes = {
        @Index(name = "idx_daily_summary_date", columnList = "summary_date")
    }
)
public class DailySummary {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "summary_date", nullable = false, unique = true)
    private LocalDate summaryDate;

    @Column(name = "initiated_count")
    private Integer initiatedCount;

    @Column(name = "initiated_amount", precision = 19, scale = 4)
    private BigDecimal initiatedAmount;

    @Column(name = "settled_count")
    private Integer settledCount;

    @Column(name = "settled_amount", precision = 19, scale = 4)
    private BigDecimal settledAmount;

    @Column(name = "fraud_alert_count")
    private Integer fraudAlertCount;

    @Column(name = "new_registrations")
    private Integer newRegistrations;

    @Column(name = "pending_settlement_amount", precision = 19, scale = 4)
    private BigDecimal pendingSettlementAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // ── Constructors ───────────────────────────────────────────────────────

    protected DailySummary() {}

    public DailySummary(LocalDate summaryDate) {
        this.summaryDate = summaryDate;
    }

    // ── Getters and setters ────────────────────────────────────────────────

    public UUID getId() { return id; }
    public LocalDate getSummaryDate() { return summaryDate; }
    public Integer getInitiatedCount() { return initiatedCount; }
    public void setInitiatedCount(Integer initiatedCount) { this.initiatedCount = initiatedCount; }
    public BigDecimal getInitiatedAmount() { return initiatedAmount; }
    public void setInitiatedAmount(BigDecimal initiatedAmount) { this.initiatedAmount = initiatedAmount; }
    public Integer getSettledCount() { return settledCount; }
    public void setSettledCount(Integer settledCount) { this.settledCount = settledCount; }
    public BigDecimal getSettledAmount() { return settledAmount; }
    public void setSettledAmount(BigDecimal settledAmount) { this.settledAmount = settledAmount; }
    public Integer getFraudAlertCount() { return fraudAlertCount; }
    public void setFraudAlertCount(Integer fraudAlertCount) { this.fraudAlertCount = fraudAlertCount; }
    public Integer getNewRegistrations() { return newRegistrations; }
    public void setNewRegistrations(Integer newRegistrations) { this.newRegistrations = newRegistrations; }
    public BigDecimal getPendingSettlementAmount() { return pendingSettlementAmount; }
    public void setPendingSettlementAmount(BigDecimal pendingSettlementAmount) { this.pendingSettlementAmount = pendingSettlementAmount; }
    public Instant getCreatedAt() { return createdAt; }
}
