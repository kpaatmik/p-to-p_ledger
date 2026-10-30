package com.paymentledger.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a P2P money transfer.
 * Maps to the {@code transactions} table.
 *
 * <p>Idempotency is enforced by the UNIQUE constraint on {@code request_id}.
 * Repeated submissions with the same {@code request_id} must return the original
 * result without creating a second transaction.
 */
@Entity
@Table(
    name = "transactions",
    indexes = {
        @Index(name = "idx_transactions_sender_id",           columnList = "sender_id"),
        @Index(name = "idx_transactions_receiver_id",         columnList = "receiver_id"),
        @Index(name = "idx_transactions_status",              columnList = "status"),
        @Index(name = "idx_transactions_settlement_due_date", columnList = "settlement_due_date"),
        @Index(name = "idx_transactions_request_id",          columnList = "request_id")
    }
)
public class Transaction {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Client-generated idempotency key. UNIQUE constraint prevents duplicate processing. */
    @Column(name = "request_id", nullable = false, unique = true, length = 255)
    private String requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private User receiver;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "remark", length = 500)
    private String remark;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private TransactionType type;

    /** Calculated as initiation date + 3 business days (weekends excluded). */
    @Column(name = "settlement_due_date")
    private LocalDate settlementDueDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "settled_at")
    private Instant settledAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // ── Constructors ───────────────────────────────────────────────────────

    protected Transaction() {}

    public Transaction(String requestId, User sender, User receiver,
                       BigDecimal amount, String remark,
                       TransactionType type, LocalDate settlementDueDate) {
        this.requestId = requestId;
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.remark = remark;
        this.type = type;
        this.status = TransactionStatus.PENDING;
        this.settlementDueDate = settlementDueDate;
    }

    // ── Getters and setters ────────────────────────────────────────────────

    public UUID getId() { return id; }
    public String getRequestId() { return requestId; }
    public User getSender() { return sender; }
    public User getReceiver() { return receiver; }
    public BigDecimal getAmount() { return amount; }
    public String getRemark() { return remark; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public TransactionType getType() { return type; }
    public LocalDate getSettlementDueDate() { return settlementDueDate; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getSettledAt() { return settledAt; }
    public void setSettledAt(Instant settledAt) { this.settledAt = settledAt; }
}
