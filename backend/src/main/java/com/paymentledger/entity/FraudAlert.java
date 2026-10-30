package com.paymentledger.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Records a fraud detection event for admin review.
 * Maps to the {@code fraud_alerts} table.
 * 
 * A fraud alert may reference either:
 * - An approved Transaction (for HIGH_VALUE alerts on successful transfers)
 * - A rejected attempt (for DAILY_LIMIT and DUPLICATE alerts on blocked transfers)
 * 
 * For rejected attempts, transaction_id is null and attempt_sender_id, attempt_recipient_id,
 * and attempt_amount store the attempt metadata directly.
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

    /**
     * For HIGH_VALUE alerts: references the approved PENDING transaction.
     * For DAILY_LIMIT/DUPLICATE rejection alerts: null (use attempt_* fields instead).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "transaction_id", nullable = true)
    private Transaction transaction;

    /**
     * For rejection alerts only: sender of the rejected attempt.
     * Null if transaction_id is set.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_sender_id")
    private User attemptSender;

    /**
     * For rejection alerts only: recipient of the rejected attempt.
     * Null if transaction_id is set.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_recipient_id")
    private User attemptRecipient;

    /**
     * For rejection alerts only: amount of the rejected attempt.
     * Null if transaction_id is set.
     */
    @Column(name = "attempt_amount", precision = 19, scale = 4)
    private BigDecimal attemptAmount;

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

    /**
     * Constructor for HIGH_VALUE alerts linked to an approved transaction.
     */
    public FraudAlert(Transaction transaction, AlertType alertType) {
        this.transaction = transaction;
        this.alertType = alertType;
    }

    /**
     * Constructor for DAILY_LIMIT and DUPLICATE rejection alerts (no transaction, use attempt metadata).
     */
    public FraudAlert(User attemptSender, User attemptRecipient, BigDecimal attemptAmount, AlertType alertType) {
        this.transaction = null;
        this.attemptSender = attemptSender;
        this.attemptRecipient = attemptRecipient;
        this.attemptAmount = attemptAmount;
        this.alertType = alertType;
    }

    // ── Getters and setters ────────────────────────────────────────────────

    public UUID getId() { return id; }
    public Transaction getTransaction() { return transaction; }
    public User getAttemptSender() { return attemptSender; }
    public User getAttemptRecipient() { return attemptRecipient; }
    public BigDecimal getAttemptAmount() { return attemptAmount; }
    public AlertType getAlertType() { return alertType; }
    public AlertStatus getStatus() { return status; }
    public void setStatus(AlertStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
    public User getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(User reviewedBy) { this.reviewedBy = reviewedBy; }
}
