package com.paymentledger.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Records every administrative action for audit purposes.
 * Maps to the {@code audit_log} table.
 * Entries are immutable once written.
 */
@Entity
@Table(
    name = "audit_log",
    indexes = {
        @Index(name = "idx_audit_log_admin_id",    columnList = "admin_id"),
        @Index(name = "idx_audit_log_performed_at", columnList = "performed_at")
    }
)
public class AuditLog {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false, updatable = false)
    private User admin;

    @Column(name = "action", nullable = false, length = 100, updatable = false)
    private String action;

    /** Entity type affected, e.g. "Wallet", "Merchant", "FraudAlert". */
    @Column(name = "target_type", length = 50, updatable = false)
    private String targetType;

    /** UUID of the affected entity. Nullable for actions with no single target. */
    @Column(name = "target_id", updatable = false)
    private UUID targetId;

    @Column(name = "details", columnDefinition = "TEXT", updatable = false)
    private String details;

    @Column(name = "performed_at", nullable = false, updatable = false)
    private Instant performedAt;

    @PrePersist
    protected void onCreate() {
        this.performedAt = Instant.now();
    }

    // ── Constructors ───────────────────────────────────────────────────────

    protected AuditLog() {}

    public AuditLog(User admin, String action, String targetType,
                    UUID targetId, String details) {
        this.admin = admin;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.details = details;
    }

    // ── Getters (immutable — no setters) ──────────────────────────────────

    public UUID getId() { return id; }
    public User getAdmin() { return admin; }
    public String getAction() { return action; }
    public String getTargetType() { return targetType; }
    public UUID getTargetId() { return targetId; }
    public String getDetails() { return details; }
    public Instant getPerformedAt() { return performedAt; }
}
