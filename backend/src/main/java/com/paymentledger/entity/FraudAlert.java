package com.paymentledger.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Records a fraud detection event for admin review.
 * Maps to the {@code fraud_alerts} table.
 */
@Entity
@Table(
    name = "fraud_alerts",
    indexes = {
        @Index(name = "idx_fraud_alerts_transaction_id", columnList = "transaction_id"),
        @Index(name = "idx_fraud_alerts_status",         columnList = "status")
    }
)
public class FraudAlert {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 30)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AlertStatus status = AlertStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    /** The admin who reviewed this alert. Null if not yet reviewed. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // ── Constructors ───────────────────────────────────────────────────────

    protected FraudAlert() {}

    public FraudAlert(Transaction transaction, AlertType alertType) {
        this.transaction = transaction;
        this.alertType = alertType;
    }

    // ── Getters and setters ────────────────────────────────────────────────

    public UUID getId() { return id; }
    public Transaction getTransaction() { return transaction; }
    public AlertType getAlertType() { return alertType; }
    public AlertStatus getStatus() { return status; }
    public void setStatus(AlertStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
    public User getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(User reviewedBy) { this.reviewedBy = reviewedBy; }
}
